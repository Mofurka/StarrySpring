package irden.space.proxy.protocol.payload.protocol_response;

import irden.space.proxy.protocol.codec.variant.VariantValue;

public record ProtocolResponse(int serverResponse,
                               VariantValue info) {
}
