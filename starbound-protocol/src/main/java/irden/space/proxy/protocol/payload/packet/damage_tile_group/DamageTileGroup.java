package irden.space.proxy.protocol.payload.packet.damage_tile_group;

import irden.space.proxy.protocol.payload.common.tile_damage.TileDamage;
import irden.space.proxy.protocol.payload.common.tile_layer.TileLayer;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2F;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2I;

import java.util.List;
import java.util.Optional;

public record DamageTileGroup(
        List<StarVec2I> tilePositions,
        TileLayer layer,
        StarVec2F sourcePosition,
        TileDamage tileDamage,
        Optional<Integer> sourceEntity
) {
}
