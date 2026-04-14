package irden.space.proxy.protocol.payload.packet.modify_tile_list.tile_modification;

import irden.space.proxy.protocol.payload.common.star_maybe.StarMaybe;
import irden.space.proxy.protocol.payload.packet.modify_tile_list.tile_modification.tile_layer.TileLayer;

public record PlaceMod(
        TileLayer layer,
        int mod,
        StarMaybe<Short> modHueShift
) implements TileModification {
}
