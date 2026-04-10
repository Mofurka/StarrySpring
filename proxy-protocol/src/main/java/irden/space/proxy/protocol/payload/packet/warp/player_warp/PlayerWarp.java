package irden.space.proxy.protocol.payload.packet.warp.player_warp;

import irden.space.proxy.protocol.payload.common.warp.action.WarpAction;

public record PlayerWarp(
        WarpAction warpAction,
        boolean deploy
) {
}
