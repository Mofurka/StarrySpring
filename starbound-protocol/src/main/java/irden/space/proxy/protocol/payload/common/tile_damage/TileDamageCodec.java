package irden.space.proxy.protocol.payload.common.tile_damage;

import irden.space.proxy.protocol.codec.BinaryCodec;
import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;

public enum TileDamageCodec implements BinaryCodec<TileDamage> {
    INSTANCE;

    @Override
    public TileDamage read(BinaryReader reader) {
        TileDamageType type = TileDamageType.fromId(reader.readUnsignedByte());
        float amount = reader.readFloat32BE();
        int harvestLevel = reader.readInt32BE();
        return new TileDamage(type, amount, harvestLevel);
    }

    @Override
    public void write(BinaryWriter writer, TileDamage value) {
        writer.writeByte((byte) value.type().id());
        writer.writeFloat32BE(value.amount());
        writer.writeInt32BE(value.harvestLevel());
    }
}
