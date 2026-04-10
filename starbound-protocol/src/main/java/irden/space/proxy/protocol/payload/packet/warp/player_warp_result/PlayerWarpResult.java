package irden.space.proxy.protocol.payload.packet.warp.player_warp_result;

import irden.space.proxy.protocol.payload.common.warp.action.WarpAction;

public record PlayerWarpResult(
        boolean warpSuccess,
        WarpAction warpAction,
        boolean warpActionInvalid
) {
}
