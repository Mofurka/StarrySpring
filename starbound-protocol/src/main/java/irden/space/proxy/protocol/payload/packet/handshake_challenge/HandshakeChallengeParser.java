package irden.space.proxy.protocol.payload.packet.handshake_challenge;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.StarByteArrayCodec;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class HandshakeChallengeParser implements PacketParser<HandshakeChallenge> {
    @Override
    public HandshakeChallenge parse(BinaryReader reader, int openProtocolVersion) {
        byte[] read = StarByteArrayCodec.read(reader);
        return new HandshakeChallenge(read);
    }

    @Override
    public byte[] write(HandshakeChallenge payload, int openProtocolVersion) {
        BinaryWriter writer = new BinaryWriter();
        StarByteArrayCodec.write(writer, payload.salt());
        return finish(writer);
    }
}
