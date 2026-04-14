package irden.space.proxy.protocol.payload.packet.modify_tile_list.tile_modification;

import irden.space.proxy.protocol.payload.packet.modify_tile_list.tile_modification.tile_layer.TileLayer;

import java.util.Optional;

public record PlaceMod(
        TileLayer layer,
        short mod,
        Optional<Integer> modHueShift
) implements TileModification {
}
