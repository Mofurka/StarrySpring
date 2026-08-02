package irden.space.proxy.protocol.payload.packet.tile_liquid_update;

import irden.space.proxy.protocol.payload.common.liquid.LiquidNetUpdate;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2I;

public record TileLiquidUpdate(
        StarVec2I position,
        LiquidNetUpdate liquidUpdate
) {
}
