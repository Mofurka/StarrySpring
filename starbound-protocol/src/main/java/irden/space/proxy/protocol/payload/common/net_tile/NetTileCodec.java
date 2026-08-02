package irden.space.proxy.protocol.payload.common.net_tile;

import irden.space.proxy.protocol.codec.BinaryCodec;
import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.VlqUnsignedCodec;
import irden.space.proxy.protocol.payload.common.collision.CollisionKind;
import irden.space.proxy.protocol.payload.common.liquid.LiquidNetUpdate;

public enum NetTileCodec implements BinaryCodec<NetTile> {
    INSTANCE;

    private static final int EMPTY_ON_WIRE = 0;

    @Override
    public NetTile read(BinaryReader reader) {
        NetTile.NetTileBuilder tile = NetTile.builder();

        int background = reader.readUInt16BE();
        if (background == EMPTY_ON_WIRE) {
            tile.background(NetTile.EMPTY_MATERIAL_ID)
                    .backgroundHueShift(0)
                    .backgroundColorVariant(NetTile.DEFAULT_MATERIAL_COLOR_VARIANT)
                    .backgroundMod(NetTile.NO_MOD_ID)
                    .backgroundModHueShift(0);
        } else {
            tile.background(background)
                    .backgroundHueShift(reader.readUnsignedByte())
                    .backgroundColorVariant(reader.readUnsignedByte());

            int backgroundMod = reader.readUInt16BE();
            if (backgroundMod == EMPTY_ON_WIRE) {
                tile.backgroundMod(NetTile.NO_MOD_ID).backgroundModHueShift(0);
            } else {
                tile.backgroundMod(backgroundMod).backgroundModHueShift(reader.readUnsignedByte());
            }
        }

        int foreground = reader.readUInt16BE();
        if (foreground == EMPTY_ON_WIRE) {
            tile.foreground(NetTile.EMPTY_MATERIAL_ID)
                    .foregroundHueShift(0)
                    .foregroundColorVariant(NetTile.DEFAULT_MATERIAL_COLOR_VARIANT)
                    .foregroundMod(NetTile.NO_MOD_ID)
                    .foregroundModHueShift(0);
        } else {
            tile.foreground(foreground)
                    .foregroundHueShift(reader.readUnsignedByte())
                    .foregroundColorVariant(reader.readUnsignedByte());

            int foregroundMod = reader.readUInt16BE();
            if (foregroundMod == EMPTY_ON_WIRE) {
                tile.foregroundMod(NetTile.NO_MOD_ID).foregroundModHueShift(0);
            } else {
                tile.foregroundMod(foregroundMod).foregroundModHueShift(reader.readUnsignedByte());
            }
        }

        tile.collision(CollisionKind.fromId(reader.readUnsignedByte()))
                .blockBiomeIndex(reader.readUnsignedByte())
                .environmentBiomeIndex(reader.readUnsignedByte());

        int liquidId = reader.readUnsignedByte();
        int liquidLevel = liquidId == LiquidNetUpdate.EMPTY_LIQUID_ID ? 0 : reader.readUnsignedByte();
        tile.liquid(new LiquidNetUpdate(liquidId, liquidLevel));

        tile.dungeonId(VlqUnsignedCodec.INSTANCE.readInt(reader));

        return tile.build();
    }

    @Override
    public void write(BinaryWriter writer, NetTile value) {
        if (value.background() == NetTile.EMPTY_MATERIAL_ID) {
            writer.writeUInt16BE(EMPTY_ON_WIRE);
        } else {
            writer.writeUInt16BE(value.background());
            writer.writeByte(value.backgroundHueShift());
            writer.writeByte(value.backgroundColorVariant());
            if (value.backgroundMod() == NetTile.NO_MOD_ID) {
                writer.writeUInt16BE(EMPTY_ON_WIRE);
            } else {
                writer.writeUInt16BE(value.backgroundMod());
                writer.writeByte(value.backgroundModHueShift());
            }
        }

        if (value.foreground() == NetTile.EMPTY_MATERIAL_ID) {
            writer.writeUInt16BE(EMPTY_ON_WIRE);
        } else {
            writer.writeUInt16BE(value.foreground());
            writer.writeByte(value.foregroundHueShift());
            writer.writeByte(value.foregroundColorVariant());
            if (value.foregroundMod() == NetTile.NO_MOD_ID) {
                writer.writeUInt16BE(EMPTY_ON_WIRE);
            } else {
                writer.writeUInt16BE(value.foregroundMod());
                writer.writeByte(value.foregroundModHueShift());
            }
        }

        writer.writeByte((byte) value.collision().id());
        writer.writeByte(value.blockBiomeIndex());
        writer.writeByte(value.environmentBiomeIndex());

        LiquidNetUpdate liquid = value.liquid();
        writer.writeByte(liquid.liquid());
        if (!liquid.isEmpty()) {
            writer.writeByte(liquid.level());
        }

        VlqUnsignedCodec.INSTANCE.write(writer, value.dungeonId());
    }
}
