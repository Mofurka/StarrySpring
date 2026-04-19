package irden.space.proxy.protocol.payload.packet.entity.type;

import irden.space.proxy.protocol.codec.variant.VariantValue;

public record StageHandEntity(
        VariantValue payload,
        Integer entityId
) implements Entity {
}
