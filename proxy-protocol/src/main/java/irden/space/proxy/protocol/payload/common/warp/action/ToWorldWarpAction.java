package irden.space.proxy.protocol.payload.common.warp.action;

import irden.space.proxy.protocol.payload.common.warp.target.WorldTarget;

public record ToWorldWarpAction(
        WorldTarget target
) implements WarpAction{
}
