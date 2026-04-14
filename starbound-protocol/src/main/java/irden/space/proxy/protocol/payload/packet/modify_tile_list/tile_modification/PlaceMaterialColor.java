package irden.space.proxy.protocol.payload.packet.modify_tile_list.tile_modification;

import irden.space.proxy.protocol.payload.packet.modify_tile_list.tile_modification.tile_layer.TileLayer;

public record PlaceMaterialColor(
        TileLayer layer,
        int color
) implements TileModification {
}
