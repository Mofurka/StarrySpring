package irden.space.proxy.protocol.payload.packet.tile_modification_failure;

import irden.space.proxy.protocol.payload.packet.modify_tile_list.TileModificationList;


public record TileModificationFailure(
        TileModificationList modificationList
) {
}
