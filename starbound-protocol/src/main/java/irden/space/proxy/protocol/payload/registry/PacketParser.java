package irden.space.proxy.protocol.payload.registry;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;

public interface PacketParser<T> {

    int UNKNOWN_OPEN_PROTOCOL_VERSION = -1;

    T parse(BinaryReader reader, int openProtocolVersion);

    byte[] write(T payload, int openProtocolVersion);

    default byte[] finish(BinaryWriter writer) {
        return writer.toByteArray();
    }
}
