package irden.space.proxy.protocol.payload.common.warp.action;

import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;

public record ToPlayerWarpAction(
        StarUuid playerIdHex
) implements WarpAction {
}
