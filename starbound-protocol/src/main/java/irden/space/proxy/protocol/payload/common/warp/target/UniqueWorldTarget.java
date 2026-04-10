package irden.space.proxy.protocol.payload.common.warp.target;

import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;

public record UniqueWorldTarget(
        String worldName,
        StarUuid instanceUuid,
        Float something, // TODO: figure out what this is
        String teleporter
) implements WorldTarget {
}
