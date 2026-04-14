package irden.space.proxy.protocol.payload.packet.modify_tile_list.tile_modification;

import irden.space.proxy.protocol.codec.BinaryCodec;
import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.VlqCodec;
import irden.space.proxy.protocol.payload.common.star_maybe.StarMaybe;
import irden.space.proxy.protocol.payload.packet.modify_tile_list.tile_modification.tile_layer.TileLayer;

import java.util.Optional;

public enum TileModificationCodec implements BinaryCodec<TileModification> {
    INSTANCE;

    @Override
    public TileModification read(BinaryReader reader) {
        int type = VlqCodec.INSTANCE.read(reader); // type
        return switch (type) {
            case 1 -> {
                int layerByte = reader.readUnsignedByte();
                TileLayer layer;
                PlaceMaterial.TileCollisionOverride collisionOverride;

                if (layerByte > 1) {
                    layer = TileLayer.FOREGROUND;
                    collisionOverride = PlaceMaterial.TileCollisionOverride.fromId(layerByte - 1);
                } else {
                    layer = TileLayer.fromId(layerByte);
                    collisionOverride = PlaceMaterial.TileCollisionOverride.NONE;
                }

                int materialId = reader.readInt16BE();

                boolean hasMaterialHue = reader.readBoolean();
                StarMaybe<Integer> materialHue;
                if (hasMaterialHue) {
                    materialHue = new StarMaybe<>(Optional.of(reader.readUnsignedByte()));
                } else {
                    materialHue = new StarMaybe<>(Optional.empty());
                }

                yield new PlaceMaterial(layer, materialId, materialHue, collisionOverride);
            }
            case 2 -> null; // TODO: Implement
            case 3 -> null; // TODO: Implement
            case 4 -> null; // TODO: Implement
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
    }

    @Override
    public void write(BinaryWriter writer, TileModification value) {

    }
}
