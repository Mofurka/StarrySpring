package irden.space.proxy.protocol.payload.common.liquid;

import irden.space.proxy.protocol.codec.BinaryCodec;
import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;

public enum LiquidNetUpdateCodec implements BinaryCodec<LiquidNetUpdate> {
    INSTANCE;

    @Override
    public LiquidNetUpdate read(BinaryReader reader) {
        int liquid = reader.readUnsignedByte();
        int level = reader.readUnsignedByte();
        return new LiquidNetUpdate(liquid, level);
    }

    @Override
    public void write(BinaryWriter writer, LiquidNetUpdate value) {
        writer.writeByte(value.liquid());
        writer.writeByte(value.level());
    }
}
