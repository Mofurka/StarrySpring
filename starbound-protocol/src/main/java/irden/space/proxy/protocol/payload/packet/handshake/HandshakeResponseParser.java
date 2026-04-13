package irden.space.proxy.protocol.payload.packet.handshake;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.StarByteArrayCodec;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class HandshakeResponseParser implements PacketParser<HandshakeResponse> {
    @Override
    public HandshakeResponse parse(BinaryReader reader, int openProtocolVersion) {
        byte[] passHash = StarByteArrayCodec.INSTANCE.read(reader);
        return new HandshakeResponse(passHash);
    }

    @Override
    public byte[] write(HandshakeResponse payload, int openProtocolVersion) {
        BinaryWriter writer = new BinaryWriter();
        StarByteArrayCodec.INSTANCE.write(writer, payload.passHash());
        return finish(writer);
    }
}
