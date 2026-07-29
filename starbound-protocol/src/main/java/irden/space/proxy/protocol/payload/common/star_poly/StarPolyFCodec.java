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
        int verticesCount = VlqUnsignedCodec.INSTANCE.read(reader);
        StarVec2F[] vertices = new StarVec2F[verticesCount];
        for (int i = 0; i < verticesCount; i++) {
            // PolyF сериализуется как writeContainer(vertexes): VLQ-длина + каждая вершина через
            // DataStream::operator<<(Vec2F) = 2× сырой float32 (BE). НЕ fixed-point:
            // NetElementData<PolyF> m_collisionPoly использует обычный DataStream, без оптимизации.
            vertices[i] = StarVec2FCodec.INSTANCE.read(reader);
        }
        return new StarPolyF(vertices);
    }

    @Override
    public void write(BinaryWriter writer, StarPolyF value) {
        StarVec2F[] vertices = value.vertices();
        VlqUnsignedCodec.INSTANCE.write(writer, vertices.length);
        for (StarVec2F vertex : vertices) {
            StarVec2FCodec.INSTANCE.write(writer, vertex);
        }
    }
}
