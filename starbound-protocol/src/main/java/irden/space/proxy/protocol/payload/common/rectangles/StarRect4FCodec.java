package irden.space.proxy.protocol.payload.common.rectangles;

import irden.space.proxy.protocol.codec.BinaryCodec;
import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;

public enum StarRect4FCodec implements BinaryCodec<StarRect4F> {
    INSTANCE;
    @Override
    public StarRect4F read(BinaryReader reader) {
        float x = reader.readFloat32BE();
        float y = reader.readFloat32BE();
        float width = reader.readFloat32BE();
        float height = reader.readFloat32BE();
        return new StarRect4F(x, y, width, height);
    }

    @Override
    public void write(BinaryWriter writer, StarRect4F value) {
        writer.writeFloat32BE(value.x());
        writer.writeFloat32BE(value.y());
        writer.writeFloat32BE(value.width());
        writer.writeFloat32BE(value.height());
    }
}
