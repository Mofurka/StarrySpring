package irden.space.proxy.protocol.payload.common.vectors;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class StarVec2FCodec {

    public static StarVec2F read(BinaryReader reader) {
        return new StarVec2F(
                reader.readFloat32BE(),
                reader.readFloat32BE()
        );
    }

    public static void write(BinaryWriter writer, StarVec2F value) {
        writer.writeFloat32BE(value.x());
        writer.writeFloat32BE(value.y());
    }
}
