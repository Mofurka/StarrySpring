package irden.space.proxy.protocol.payload.packet.modify_tile_list.tile_modification;

public record PlaceLiquid(
        short liquidId,
        float liquidLevel
) implements TileModification{
}
