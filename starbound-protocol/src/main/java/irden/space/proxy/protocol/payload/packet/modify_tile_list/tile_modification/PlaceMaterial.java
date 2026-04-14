package irden.space.proxy.protocol.payload.packet.modify_tile_list.tile_modification;

import irden.space.proxy.protocol.payload.packet.modify_tile_list.tile_modification.tile_layer.TileLayer;

import java.util.Optional;

public record PlaceMaterial(
        TileLayer layer,
        short materialId,
        Optional<Integer> materialHue,
        TileCollisionOverride collisionOverride

) implements TileModification {
    public enum TileCollisionOverride {
        NONE(0),
        EMPTY(1),
        PLATFORM(2),
        BLOCK(3);

        private final int id;

        TileCollisionOverride(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
        public static TileCollisionOverride fromId(int id) {
            for (TileCollisionOverride collisionOverride : values()) {
                if (collisionOverride.id == id) {
                    return collisionOverride;
                }
            }
            throw new IllegalArgumentException("Unknown TileCollisionOverride id: " + id);
        }
    }
}

