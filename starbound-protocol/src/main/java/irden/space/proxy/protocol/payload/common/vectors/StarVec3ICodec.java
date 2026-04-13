package irden.space.proxy.protocol.payload.common.vectors;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class StarVec3ICodec {

    public static StarVec3I read(BinaryReader reader) {
        int x = reader.readInt32BE();
        int y = reader.readInt32BE();
        int z = reader.readInt32BE();
        return new StarVec3I(x, y, z);
    }

    public static void write(BinaryWriter writer, StarVec3I value) {
        writer.writeInt32BE(value.x());
        writer.writeInt32BE(value.y());
        writer.writeInt32BE(value.z());
    }
}
