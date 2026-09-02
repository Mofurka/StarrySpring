package irden.space.proxy.protocol.payload.common.tile_damage;

import irden.space.proxy.protocol.codec.BinaryCodec;
import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2F;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2FCodec;

public enum TileDamageStatusCodec implements BinaryCodec<TileDamageStatus> {
    INSTANCE;

    @Override
    public TileDamageStatus read(BinaryReader reader) {
        float damagePercentage = reader.readFloat32BE();
        float damageEffectTimeFactor = reader.readFloat32BE();
        boolean harvested = reader.readBoolean();
        StarVec2F damageSourcePosition = StarVec2FCodec.INSTANCE.read(reader);
        TileDamageType damageType = TileDamageType.fromId(reader.readUnsignedByte());
        return new TileDamageStatus(damagePercentage, damageEffectTimeFactor, harvested, damageSourcePosition, damageType);
    }

    @Override
    public void write(BinaryWriter writer, TileDamageStatus value) {
        writer.writeFloat32BE(value.damagePercentage());
        writer.writeFloat32BE(value.damageEffectTimeFactor());
        writer.writeBoolean(value.harvested());
        StarVec2FCodec.INSTANCE.write(writer, value.damageSourcePosition());
        writer.writeByte((byte) value.damageType().id());
    }
}
