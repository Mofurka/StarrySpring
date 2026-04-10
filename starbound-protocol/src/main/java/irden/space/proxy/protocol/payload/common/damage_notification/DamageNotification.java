package irden.space.proxy.protocol.payload.common.damage_notification;

import irden.space.proxy.protocol.payload.common.vectors.StarVec2F;
import irden.space.proxy.protocol.payload.packet.damage_notification.consts.HitType;

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
