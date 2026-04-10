package irden.space.proxy.protocol.payload.packet.damage_notification;

import irden.space.proxy.protocol.payload.common.damage_notification.DamageNotification;

public record RemoteDamageNotification(
        int entityId,
        DamageNotification damageNotification
) {
}
