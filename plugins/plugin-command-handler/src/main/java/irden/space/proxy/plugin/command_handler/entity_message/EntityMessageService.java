package irden.space.proxy.plugin.command_handler.entity_message;

import irden.space.proxy.plugin.api.PluginSessionContext;
import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.packet.PacketType;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;
import irden.space.proxy.protocol.payload.packet.entity_message.EntityIdTarget;
import irden.space.proxy.protocol.payload.packet.entity_message.EntityMessage;
import irden.space.proxy.protocol.payload.packet.entity_message.EntityMessageTarget;
import irden.space.proxy.protocol.payload.packet.entity_message.UniqueEntityIdTarget;
import irden.space.proxy.protocol.payload.packet.entity_message_response.EntityMessageRsponseValue;
import irden.space.proxy.protocol.payload.packet.entity_message_response.FailedEntityMessageResponse;
import irden.space.proxy.protocol.payload.packet.entity_message_response.SuccessEntityMessageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class EntityMessageService {

    public static final int SERVER_CONNECTION_ID = 0;

    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private final Map<StarUuid, PendingRequest> pending = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "entity-message-timeout");
        thread.setDaemon(true);
        return thread;
    });


    /** Отправить сообщение на entityId игрока (от имени сервера, с таймаутом по умолчанию). */
    public CompletableFuture<VariantValue> sendToEntity(
            PluginSessionContext session, int entityId, String message, VariantValue... args) {
        return send(session, new EntityIdTarget(entityId), message, args);
    }

    /** Отправить сообщение на unique entity uuid (от имени сервера, с таймаутом по умолчанию). */
    public CompletableFuture<VariantValue> sendToUuid(
            PluginSessionContext session, StarUuid entityUuid, String message, VariantValue... args) {
        return send(session, new UniqueEntityIdTarget(entityUuid), message, args);
    }

    public CompletableFuture<VariantValue> send(
            PluginSessionContext session, EntityMessageTarget target, String message, VariantValue... args) {
        return send(session, target, message, args, SERVER_CONNECTION_ID, DEFAULT_TIMEOUT);
    }

    /**
     * Полная форма: можно задать id соединения-отправителя и таймаут ожидания ответа.
     *
     * @return future с телом успешного ответа; падает с {@link EntityMessageFailedException}
     *         или {@link EntityMessageTimeoutException}
     */
    public CompletableFuture<VariantValue> send(
            PluginSessionContext session,
            EntityMessageTarget target,
            String message,
            VariantValue[] args,
            int fromConnection,
            Duration timeout
    ) {
        StarUuid uuid = StarUuid.fromJavaUuid(UUID.randomUUID());
        CompletableFuture<VariantValue> future = new CompletableFuture<>();

        ScheduledFuture<?> timeoutTask = scheduler.schedule(
                () -> timeout(uuid, message, timeout),
                timeout.toMillis(),
                TimeUnit.MILLISECONDS
        );

        pending.put(uuid, new PendingRequest(session.sessionId(), message, future, timeoutTask));

        try {
            session.sendToClient(
                    PacketType.ENTITY_MESSAGE,
                    new EntityMessage(target, message, args == null ? new VariantValue[0] : args, uuid, fromConnection)
            );
        } catch (RuntimeException e) {
            PendingRequest request = pending.remove(uuid);
            if (request != null) {
                request.timeoutTask().cancel(false);
            }
            future.completeExceptionally(e);
        }

        return future;
    }


    boolean complete(StarUuid uuid, EntityMessageRsponseValue value) {
        PendingRequest request = pending.remove(uuid);
        if (request == null) {
            return false; // не наш запрос - пусть идёт своим путём
        }

        request.timeoutTask().cancel(false);

        switch (value) {
            case SuccessEntityMessageResponse(VariantValue response) -> request.future().complete(response);
            case FailedEntityMessageResponse(String error) ->
                    request.future().completeExceptionally(new EntityMessageFailedException(request.message(), error));
        }

        return true;
    }

    void purgeSession(String sessionId) {
        pending.entrySet().removeIf(entry -> {
            PendingRequest request = entry.getValue();
            if (!request.sessionId().equals(sessionId)) {
                return false;
            }
            request.timeoutTask().cancel(false);
            request.future().completeExceptionally(
                    new EntityMessageFailedException(request.message(), "session closed"));
            return true;
        });
    }

    private void timeout(StarUuid uuid, String message, Duration timeout) {
        PendingRequest request = pending.remove(uuid);
        if (request == null) {
            return;
        }
        log.debug("EntityMessage '{}' timed out after {}", message, timeout);
        request.future().completeExceptionally(new EntityMessageTimeoutException(message, timeout));
    }

    private record PendingRequest(
            String sessionId,
            String message,
            CompletableFuture<VariantValue> future,
            ScheduledFuture<?> timeoutTask
    ) {
    }
}
