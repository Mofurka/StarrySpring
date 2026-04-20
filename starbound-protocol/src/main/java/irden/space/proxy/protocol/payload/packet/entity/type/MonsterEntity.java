package irden.space.proxy.protocol.payload.packet.entity.type;

import irden.space.proxy.protocol.codec.variant.VariantValue;

public record MonsterEntity(
        VariantValue payload
) implements Entity {
}
