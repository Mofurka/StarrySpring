package irden.space.proxy.protocol.payload.packet.modify_tile_list;

import irden.space.proxy.protocol.payload.common.star_pair.StarPair;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2I;
import irden.space.proxy.protocol.payload.packet.modify_tile_list.tile_modification.TileModification;

import java.util.List;

public record TileModificationList(
        List<StarPair<StarVec2I, TileModification>> modifications
) {
}
