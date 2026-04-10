package irden.space.proxy.protocol.payload.packet.entity_message_response;

import irden.space.proxy.protocol.codec.variant.VariantValue;

public record SuccessEntityMessageResponse(
        VariantValue response
) implements EntityMessageRsponseValue {
}
