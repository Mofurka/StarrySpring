package irden.space.proxy.protocol.payload.handshake_challenge;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.StarByteArrayCodec;
import irden.space.proxy.protocol.payload.PacketParser;

public class HandshakeChallengeParser implements PacketParser<HandshakeChallenge> {
    @Override
    public HandshakeChallenge parse(BinaryReader reader) {
        byte[] read = StarByteArrayCodec.read(reader);
        return new HandshakeChallenge(read);
    }

    @Override
    public byte[] write(HandshakeChallenge payload) {
        BinaryWriter writer = new BinaryWriter();
        StarByteArrayCodec.write(writer, payload.salt());
        return finish(writer);
    }
}
