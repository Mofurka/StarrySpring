package irden.space.proxy.protocol.payload.packet.handshake;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.StarByteArrayCodec;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class HandshakeResponseParser implements PacketParser<HandshakeResponse> {
    @Override
    public HandshakeResponse parse(BinaryReader reader) {
        byte[] passHash = StarByteArrayCodec.INSTANCE.read(reader);
        return new HandshakeResponse(passHash);
    }

    @Override
    public byte[] write(BinaryWriter writer, HandshakeResponse payload) {
        StarByteArrayCodec.INSTANCE.write(writer, payload.passHash());
        return finish(writer);
    }
}
