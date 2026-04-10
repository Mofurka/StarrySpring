package irden.space.proxy.protocol.payload.common.star_uuid;

import irden.space.proxy.protocol.codec.BinaryCodec;
import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;

public enum StarUuidCodec implements BinaryCodec<StarUuid> {
    INSTANCE;

    @Override
    public StarUuid read(BinaryReader reader) {
        return new StarUuid(reader.readBytes(16));
    }

    @Override
    public void write(BinaryWriter writer, StarUuid value) {
        writer.writeBytes(value.bytes());
    }
}
