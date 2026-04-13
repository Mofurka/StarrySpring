package irden.space.proxy.protocol.payload.packet.damage;

import irden.space.proxy.protocol.payload.common.damage.DamageRequest;

public record RemoteDamageRequest(
        int sourceId,
        int targetId,
        DamageRequest damageRequest
) {
}
