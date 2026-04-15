package irden.space.proxy.protocol.payload.registry;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;

public interface PacketParser<T> {

    int LEGACY_PROTOCOL_VERSION = -1;

    T parse(BinaryReader reader);

    byte[] write(BinaryWriter writer, T payload);

    default byte[] finish(BinaryWriter writer) {
        return writer.toByteArray();
    }
}
