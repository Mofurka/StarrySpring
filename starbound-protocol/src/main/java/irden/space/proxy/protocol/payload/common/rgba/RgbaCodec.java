package irden.space.proxy.protocol.payload.common.rgba;

import irden.space.proxy.protocol.codec.BinaryCodec;
import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;

public enum RgbaCodec implements BinaryCodec<Rgba> {
    INSTANCE;

    @Override
    public Rgba read(BinaryReader reader) {
        int r = reader.readUnsignedByte();
        int g = reader.readUnsignedByte();
        int b = reader.readUnsignedByte();
        int a = reader.readUnsignedByte();
        return new Rgba(r, g, b, a);
    }

    @Override
    public void write(BinaryWriter writer, Rgba value) {
        writer.writeByte(value.r());
        writer.writeByte(value.g());
        writer.writeByte(value.b());
        writer.writeByte(value.a());
    }
}
