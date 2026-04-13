package irden.space.proxy.protocol.payload.common.vectors;

import irden.space.proxy.protocol.codec.BinaryCodec;
import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;

public enum StarVec2ICodec implements BinaryCodec<StarVec2I> {
    INSTANCE;

    @Override
    public StarVec2I read(BinaryReader reader) {
        int x = reader.readInt32BE();
        int y = reader.readInt32BE();
        return new StarVec2I(x, y);
    }

    @Override
    public void write(BinaryWriter writer, StarVec2I value) {
        writer.writeInt32BE(value.x());
        writer.writeInt32BE(value.y());
    }
}
