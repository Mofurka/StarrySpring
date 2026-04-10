package irden.space.proxy.protocol.payload.common.warp.action;

public record ToAliasWarpAction(
        int aliasId
) implements WarpAction{
}
