package irden.space.proxy.protocol.payload.common.star_poly;

import irden.space.proxy.protocol.codec.BinaryCodec;
import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.VlqUnsignedCodec;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2F;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2FCodec;

public enum StarPolyFCodec implements BinaryCodec<StarPolyF> {
    INSTANCE;

    @Override
    public StarPolyF read(BinaryReader reader) {
        int verticesCount  = VlqUnsignedCodec.INSTANCE.read(reader);
        StarVec2F[] vertices = new StarVec2F[verticesCount];
        for (int i = 0; i < verticesCount; i++) {
            StarVec2F vertex = StarVec2FCodec.INSTANCE.readFixedPointBased(reader, 0.003125f);
            vertices[i] = vertex;
        }
        return new StarPolyF(vertices);
    }

    @Override
    public void write(BinaryWriter writer, StarPolyF value) {

    }
}
