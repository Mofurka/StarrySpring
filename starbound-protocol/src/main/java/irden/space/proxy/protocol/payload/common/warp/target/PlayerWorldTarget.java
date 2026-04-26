package irden.space.proxy.protocol.payload.common.warp.target;

import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;
import irden.space.proxy.protocol.payload.packet.warp.consts.SpawnTarget;

public record PlayerWorldTarget(
        StarUuid shipUuid,
        SpawnTarget spawnTarget
) implements WorldTarget{
}
