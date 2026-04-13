package irden.space.proxy.protocol.payload.common.damage;

import irden.space.proxy.protocol.payload.common.damage.consts.HitType;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2F;

public record DamageNotification(
        int sourceEntityId,
        int targetEntityId,
        StarVec2F position,
        float damageDealt,
        float healthLost,
        HitType hitType,
        String damageSourceKind,
        String targetMaterialKind
) {
}
