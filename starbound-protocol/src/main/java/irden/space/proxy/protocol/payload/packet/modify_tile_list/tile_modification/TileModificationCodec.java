package irden.space.proxy.protocol.payload.packet.modify_tile_list.tile_modification;

import irden.space.proxy.protocol.codec.BinaryCodec;
import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.VlqUnsignedCodec;
import irden.space.proxy.protocol.payload.packet.modify_tile_list.tile_modification.tile_layer.TileLayer;

import java.util.Optional;

public enum TileModificationCodec implements BinaryCodec<TileModification> {
    INSTANCE;

    @Override
    public TileModification read(BinaryReader reader) {
        int type = VlqUnsignedCodec.INSTANCE.read(reader); // type
        return switch (type) {
            case 1 -> readPlaceMaterial(reader);
            case 2 -> readPlaceMod(reader);
            case 3 -> readPlaceMaterialColor(reader);
            case 4 -> readPlaceLiquid(reader);
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
    }

    @Override
    public void write(BinaryWriter writer, TileModification value) {
        switch (value) {
            case PlaceMaterial placeMaterial -> {
                VlqUnsignedCodec.INSTANCE.write(writer, 1);
                writePlaceMaterial(writer, placeMaterial);
            }
            case PlaceMod placeMod -> {
                VlqUnsignedCodec.INSTANCE.write(writer, 2);
                writePlaceMod(writer, placeMod);
            }
            case PlaceMaterialColor placeMaterialColor -> {
                VlqUnsignedCodec.INSTANCE.write(writer, 3);
                writePlaceMaterialColor(writer, placeMaterialColor);
            }
            case PlaceLiquid placeLiquid -> {
                VlqUnsignedCodec.INSTANCE.write(writer, 4);
                writePlaceLiquid(writer, placeLiquid);
            }
        }
    }

    private Optional<Integer> readMaterialHue(BinaryReader reader) {
        boolean hasMaterialHue = reader.readBoolean();
        if (hasMaterialHue) {
            return Optional.of(reader.readUnsignedByte());
        } else {
            return Optional.empty();
        }
    }
    private void writeMaterialHue(BinaryWriter writer, Optional<Integer> materialHue) {
        if (materialHue.isPresent()) {
            writer.writeBoolean(true);
            writer.writeByte(materialHue.get());
        } else {
            writer.writeBoolean(false);
        }
    }

    private PlaceMaterial readPlaceMaterial(BinaryReader reader)
    {
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

        short materialId = reader.readInt16BE();
        Optional<Integer> materialHue = readMaterialHue(reader);
        return new PlaceMaterial(layer, materialId, materialHue, collisionOverride);
    }
    private void writePlaceMaterial(BinaryWriter writer, PlaceMaterial value) {
        int layerByte = switch (value.layer()) {
            case BACKGROUND -> 0;
            case FOREGROUND -> 1;
        };
        if (value.collisionOverride() != PlaceMaterial.TileCollisionOverride.NONE) {
            layerByte += value.collisionOverride().id();
        }
        writer.writeByte((byte) layerByte);
        writer.writeInt16BE(value.materialId());
        writeMaterialHue(writer, value.materialHue());
    }
    private PlaceMod readPlaceMod(BinaryReader reader) {
        TileLayer tileLayer = TileLayer.fromId(reader.readUnsignedByte());
        short mod = reader.readInt16BE();
        Optional<Integer> materialHue = readMaterialHue(reader);
        return new PlaceMod(tileLayer, mod, materialHue);
    }

    private void writePlaceMod(BinaryWriter writer, PlaceMod value) {
        writer.writeByte((byte) value.layer().id());
        writer.writeInt16BE(value.mod());
        writeMaterialHue(writer, value.modHueShift());
    }

    private PlaceMaterialColor readPlaceMaterialColor(BinaryReader reader) {
        TileLayer tileLayer = TileLayer.fromId(reader.readUnsignedByte());
        int colorVariant = reader.readUnsignedByte();
        return new PlaceMaterialColor(tileLayer, colorVariant);
    }

    private void writePlaceMaterialColor(BinaryWriter writer, PlaceMaterialColor value) {
        writer.writeByte((byte) value.layer().id());
        writer.writeByte(value.color());
    }

    private PlaceLiquid readPlaceLiquid(BinaryReader reader) {
        int liquidId = reader.readUnsignedByte();
        float liquidLevel = reader.readFloat32BE();
        return new PlaceLiquid(liquidId, liquidLevel);
    }

    private void writePlaceLiquid(BinaryWriter writer, PlaceLiquid value) {
        writer.writeByte(value.liquidId());
        writer.writeFloat32BE(value.liquidLevel());
    }
}
