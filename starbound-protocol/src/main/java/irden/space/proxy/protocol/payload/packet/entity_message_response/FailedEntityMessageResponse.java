package irden.space.proxy.protocol.payload.packet.entity_message_response;

public record FailedEntityMessageResponse(
        String error
) implements EntityMessageRsponseValue {
}
