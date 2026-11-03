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
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.*;

@Component
@Slf4j
public class EntityMessageService implements DisposableBean {

    public static final int SERVER_CONNECTION_ID = 0;

    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private final Map<StarUuid, PendingRequest> pending = new ConcurrentHashMap<>();

    private final Map<String, SessionOutbox> outboxes = new ConcurrentHashMap<>();

    private final ExecutorService executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual()
            .name("entity-message-", 0)
            .uncaughtExceptionHandler((thread, failure) ->
                    log.error("EntityMessage thread {} failed", thread.getName(), failure))
            .factory());

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "entity-message-timeout");
        thread.setDaemon(true);
        return thread;
    });


    /**
     * Отправить сообщение на entityId игрока (от имени сервера, с таймаутом по умолчанию).
     */
    public CompletableFuture<VariantValue> sendToEntity(
            PluginSessionContext session, int entityId, String message, VariantValue... args) {
        return send(session, new EntityIdTarget(entityId), message, args);
    }

    /**
     * Отправить сообщение на unique entity uuid (от имени сервера, с таймаутом по умолчанию).
     */
    public CompletableFuture<VariantValue> sendToUuid(
            PluginSessionContext session, String entityUuid, String message, VariantValue... args) {
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
     * или {@link EntityMessageTimeoutException}
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

        ScheduledFuture<?> timeoutTask;
        try {
            timeoutTask = scheduler.schedule(
                    () -> timeout(uuid, message, timeout),
                    timeout.toMillis(),
                    TimeUnit.MILLISECONDS
            );
        } catch (RejectedExecutionException e) {
            return CompletableFuture.failedFuture(e);
        }

        pending.put(uuid, new PendingRequest(session.sessionId(), message, future, timeoutTask));

        EntityMessage packet = new EntityMessage(
                target, message, args == null ? new VariantValue[0] : args, uuid, fromConnection);
        try {
            enqueue(session.sessionId(), () -> write(session, uuid, packet));
        } catch (RejectedExecutionException e) {
            fail(uuid, e);
        }

        return future;
    }


    boolean complete(StarUuid uuid, EntityMessageRsponseValue value) {
        PendingRequest request = pending.remove(uuid);
        if (request == null) {
            return false; // не наш запрос - пусть идёт своим путём
        }

        request.timeoutTask().cancel(false);

        completeAsync(() -> {
            switch (value) {
                case SuccessEntityMessageResponse(VariantValue response) -> request.future().complete(response);
                case FailedEntityMessageResponse(String error) -> request.future()
                        .completeExceptionally(new EntityMessageFailedException(request.message(), error));
            }
        });

        return true;
    }

    void purgeSession(String sessionId) {
        outboxes.remove(sessionId);

        pending.entrySet().removeIf(entry -> {
            PendingRequest request = entry.getValue();
            if (!request.sessionId().equals(sessionId)) {
                return false;
            }
            request.timeoutTask().cancel(false);
            completeAsync(() -> request.future().completeExceptionally(
                    new EntityMessageSessionClosedException(request.message(), "session closed")));
            return true;
        });
    }

    @Override
    public void destroy() {
        scheduler.shutdownNow();
        executor.shutdown();
    }

    private void timeout(StarUuid uuid, String message, Duration timeout) {
        PendingRequest request = pending.remove(uuid);
        if (request == null) {
            return;
        }
        log.debug("EntityMessage '{}' timed out after {}", message, timeout);
        completeAsync(() -> request.future().completeExceptionally(new EntityMessageTimeoutException(message, timeout)));
    }

    private void write(PluginSessionContext session, StarUuid uuid, EntityMessage packet) {
        if (!pending.containsKey(uuid)) {
            return;
        }

        try {
            session.sendToClient(PacketType.ENTITY_MESSAGE, packet);
        } catch (RuntimeException e) {
            fail(uuid, e);
        }
    }

    private void fail(StarUuid uuid, Throwable failure) {
        PendingRequest request = pending.remove(uuid);
        if (request == null) {
            return;
        }
        request.timeoutTask().cancel(false);
        completeAsync(() -> request.future().completeExceptionally(failure));
    }

    private void completeAsync(Runnable completion) {
        try {
            executor.execute(completion);
        } catch (RejectedExecutionException e) {
            completion.run();
        }
    }

    private void enqueue(String sessionId, Runnable write) {
        boolean[] startDrain = new boolean[1];

        SessionOutbox outbox = outboxes.compute(sessionId, (id, current) -> {
            SessionOutbox target = current != null ? current : new SessionOutbox(id);
            target.writes.add(write);
            if (!target.draining) {
                target.draining = true;
                startDrain[0] = true;
            }
            return target;
        });

        if (startDrain[0]) {
            try {
                executor.execute(outbox::drain);
            } catch (RejectedExecutionException e) {
                outboxes.remove(sessionId, outbox);
                throw e;
            }
        }
    }

    private record PendingRequest(
            String sessionId,
            String message,
            CompletableFuture<VariantValue> future,
            ScheduledFuture<?> timeoutTask
    ) {
    }

    private final class SessionOutbox {
        private final String sessionId;
        private final Queue<Runnable> writes = new ArrayDeque<>();
        private boolean draining;

        private SessionOutbox(String sessionId) {
            this.sessionId = sessionId;
        }

        private void drain() {
            Runnable write;
            while ((write = next()) != null) {
                write.run();
            }
        }

        private Runnable next() {
            Runnable[] next = new Runnable[1];

            outboxes.compute(sessionId, (ignored, current) -> {
                next[0] = writes.poll();
                if (next[0] != null) {
                    return current;
                }
                draining = false;
                return current == this ? null : current;
            });

            return next[0];
        }
    }
}
