package irden.space.proxy.protocol.payload.common.vectors;

import irden.space.proxy.protocol.codec.BinaryCodec;
import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;

public enum StarVec2FCodec implements BinaryCodec<StarVec2F> {
    INSTANCE;

    @Override
    public StarVec2F read(BinaryReader reader) {
        return new StarVec2F(
                reader.readFloat32BE(),
                reader.readFloat32BE()
        );
    }

    @Override
    public void write(BinaryWriter writer, StarVec2F value) {
        writer.writeFloat32BE(value.x());
        writer.writeFloat32BE(value.y());
    }
}
