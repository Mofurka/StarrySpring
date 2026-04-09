package irden.space.proxy.protocol.payload;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;

public interface PacketParser<T> {

    T parse(BinaryReader reader);

    byte[] write(T payload);

    default byte[] finish(BinaryWriter writer) {
        return writer.toByteArray();
    }
}
