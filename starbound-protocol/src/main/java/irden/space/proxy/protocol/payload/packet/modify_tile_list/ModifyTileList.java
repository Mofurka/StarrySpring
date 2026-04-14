package irden.space.proxy.protocol.payload.packet.modify_tile_list;

public record ModifyTileList(
        TileModificationList modificationList,
        boolean allowEntityOverlap
) {
}
