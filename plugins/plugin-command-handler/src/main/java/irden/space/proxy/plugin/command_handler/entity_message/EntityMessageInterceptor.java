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
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class EntityMessageInterceptor {

    private final EntityMessageService entityMessageService;

    @PacketHandler(value = PacketType.ENTITY_MESSAGE, direction = PacketDirection.TO_SERVER)
    @SuppressWarnings("unused")
    public PacketDecision onEntityMessage(PacketInterceptionContext context) {
        EntityMessage entityMessage = context.parsedPayload(EntityMessage.class);
        if (entityMessage == null) {
            return PacketDecision.forward();
        }

        RegisteredEntityMessageHandler handler = EntityMessageRegistry.global().find(entityMessage.message());
        if (handler == null) {
            return PacketDecision.forward(); // не наше сообщение - пусть идёт на сервер
        }

        EntityMessageContext messageContext = new EntityMessageContext(
                context.session(),
                entityMessage.message(),
                entityMessage.args(),
                entityMessage.uuid(),
                entityMessage.entityId(),
                entityMessage.fromConnection()
        );

        EntityMessageRsponseValue response;
        try {
            VariantValue result = handler.invoke(messageContext);
            response = new SuccessEntityMessageResponse(result);
        } catch (Exception e) {
            log.error("EntityMessage handler '{}' from plugin '{}' failed",
                    entityMessage.message(), handler.ownerPluginId(), e);
            response = new FailedEntityMessageResponse(
                    e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }

        context.session().sendToClient(
                PacketType.ENTITY_MESSAGE_RESPONSE,
                new EntityMessageResponse(response, entityMessage.uuid())
        );

        return PacketDecision.cancel();
    }

    @PacketHandler(value = PacketType.ENTITY_MESSAGE_RESPONSE, direction = PacketDirection.TO_SERVER)
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
