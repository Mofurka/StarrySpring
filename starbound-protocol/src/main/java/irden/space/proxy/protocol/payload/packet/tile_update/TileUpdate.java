package irden.space.proxy.protocol.payload.packet.tile_update;

import irden.space.proxy.protocol.payload.common.net_tile.NetTile;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2I;

public record TileUpdate(
        StarVec2I position,
        NetTile tile
) {
}
