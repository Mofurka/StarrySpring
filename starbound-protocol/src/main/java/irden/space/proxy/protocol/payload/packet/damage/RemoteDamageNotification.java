package irden.space.proxy.protocol.payload.packet.damage;

import irden.space.proxy.protocol.payload.common.damage.DamageNotification;

public record RemoteDamageNotification(
        int entityId,
        DamageNotification damageNotification
) {
}
