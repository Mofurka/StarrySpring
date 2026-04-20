package irden.space.proxy.protocol.payload.packet.entity.type;

import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.payload.common.damage.DamageTeam;

public record ProjectileEntity(
        String name,
        VariantValue parameters,
        Integer entityId,
        Boolean trackSourceEntity,
        Float initialSpeed,
        Float powerMultiplier,
        DamageTeam damageTeam


) implements Entity {
}
