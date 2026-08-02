package irden.space.proxy.protocol.payload.packet.tile_damage_update;

import irden.space.proxy.protocol.payload.common.tile_damage.TileDamageStatus;
import irden.space.proxy.protocol.payload.common.tile_layer.TileLayer;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2I;

public record TileDamageUpdate(
        StarVec2I position,
        TileLayer layer,
        TileDamageStatus tileDamage
) {
}
