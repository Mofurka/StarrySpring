package irden.space.proxy.plugin.command_handler.entity_message;

import irden.space.proxy.plugin.api.PacketDecision;
import irden.space.proxy.plugin.api.PacketInterceptionContext;
import irden.space.proxy.plugin.api.PluginSessionContext;
import irden.space.proxy.plugin.api.annotations.OnDisconnected;
import irden.space.proxy.plugin.api.annotations.PacketHandler;
import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketType;
import irden.space.proxy.protocol.payload.packet.entity_message.EntityMessage;
import irden.space.proxy.protocol.payload.packet.entity_message_response.EntityMessageResponse;
import irden.space.proxy.protocol.payload.packet.entity_message_response.EntityMessageRsponseValue;
import irden.space.proxy.protocol.payload.packet.entity_message_response.FailedEntityMessageResponse;
import irden.space.proxy.protocol.payload.packet.entity_message_response.SuccessEntityMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;


@Component
@RequiredArgsConstructor
@Slf4j
public class EntityMessageInterceptor implements DisposableBean {

    private final EntityMessageService entityMessageService;
    private final ExecutorService handlerExecutor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual()
            .name("entity-message-handler-", 0)
            .uncaughtExceptionHandler((thread, failure) ->
                    log.error("EntityMessage handler thread {} failed", thread.getName(), failure))
            .factory());

    @PacketHandler(value = PacketType.ENTITY_MESSAGE)
    @SuppressWarnings("unused")
    public PacketDecision onEntityMessage(PacketInterceptionContext context) {
        EntityMessage entityMessage = context.parsedPayload(EntityMessage.class);
        if (entityMessage == null) {
            return PacketDecision.forward();
        }

        RegisteredEntityMessageHandler handler = EntityMessageRegistry.global().find(entityMessage.message());
        if (handler == null) {
            return PacketDecision.forward();
        }

        EntityMessageContext messageContext = new EntityMessageContext(
                context.session(),
                entityMessage.message(),
                entityMessage.args(),
                entityMessage.uuid(),
                entityMessage.entityId(),
                entityMessage.fromConnection()
        );
        PacketDirection direction = context.direction();

        try {
            handlerExecutor.execute(() -> handle(handler, messageContext, direction));
        } catch (RejectedExecutionException e) {
            respond(messageContext, direction, new FailedEntityMessageResponse("EntityMessage handlers are stopped"));
        }

        return PacketDecision.cancel();
    }

    private void handle(RegisteredEntityMessageHandler handler, EntityMessageContext messageContext, PacketDirection direction) {
        EntityMessageRsponseValue response;
        try {
            VariantValue result = handler.invoke(messageContext);
            response = new SuccessEntityMessageResponse(result);
        } catch (Exception e) {
            log.error("EntityMessage handler '{}' from plugin '{}' failed",
                    messageContext.message(), handler.ownerPluginId(), e);
            response = new FailedEntityMessageResponse(
                    e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }

        respond(messageContext, direction, response);
    }

    private void respond(EntityMessageContext messageContext, PacketDirection direction, EntityMessageRsponseValue response) {
        var packetResponse = new EntityMessageResponse(response, messageContext.uuid());

        try {
            if (direction == PacketDirection.TO_CLIENT) {
                messageContext.session().sendToServer(
                        PacketType.ENTITY_MESSAGE_RESPONSE,
                        packetResponse
                );

            } else {
                messageContext.session().sendToClient(
                        PacketType.ENTITY_MESSAGE_RESPONSE,
                        packetResponse
                );
            }
        } catch (RuntimeException e) {
            log.warn("Failed to send response for EntityMessage '{}' in session {}",
                    messageContext.message(), messageContext.session().sessionId(), e);
        }
    }

    @Override
    public void destroy() {
        handlerExecutor.shutdown();
    }

    @PacketHandler(value = PacketType.ENTITY_MESSAGE_RESPONSE)
    @SuppressWarnings("unused")
    public PacketDecision onEntityMessageResponse(PacketInterceptionContext context) {
        EntityMessageResponse response = context.parsedPayload(EntityMessageResponse.class);
        if (response == null) {
            return PacketDecision.forward();
        }

        boolean ours = entityMessageService.complete(response.uuid(), response.response());
        return ours ? PacketDecision.cancel() : PacketDecision.forward();
    }

    @OnDisconnected
    @SuppressWarnings("unused")
    public void onDisconnected(PluginSessionContext session) {
        entityMessageService.purgeSession(session.sessionId());
    }
}
