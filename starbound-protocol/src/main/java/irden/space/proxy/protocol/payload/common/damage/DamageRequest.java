package irden.space.proxy.protocol.payload.common.damage;

import irden.space.proxy.protocol.payload.common.damage.consts.DamageType;
import irden.space.proxy.protocol.payload.common.damage.consts.HitType;
import irden.space.proxy.protocol.payload.common.ephemeral_status_effect.EphemeralStatusEffect;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2F;

import java.util.List;

public record DamageRequest(
        HitType hitType,
        DamageType type,
        StarVec2F knockbackMomentum,
        int sourceEntityId,
        String damageSourceKind,
        List<EphemeralStatusEffect> statusEffects
) {
}
