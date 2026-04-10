package irden.space.proxy.protocol.payload.common.warp.target;

import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;

public record PlayerWorldTarget(
        StarUuid shipUuid,
        Integer posX,
        Integer posY
) implements WorldTarget{
}
