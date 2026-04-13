package irden.space.proxy.protocol.payload.common.star_uuid;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class StarUuidCodec {

    public static StarUuid read(BinaryReader reader) {
        return new StarUuid(reader.readBytes(16));
    }

    public static void write(BinaryWriter writer, StarUuid value) {
        writer.writeBytes(value.bytes());
    }
}
