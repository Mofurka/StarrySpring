package irden.space.proxy.protocol.payload.packet.handshake;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.StarByteArrayCodec;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class HandshakeChallengeParser implements PacketParser<HandshakeChallenge> {
    @Override
    public HandshakeChallenge parse(BinaryReader reader) {
        byte[] read = StarByteArrayCodec.INSTANCE.read(reader);
        return new HandshakeChallenge(read);
    }

    @Override
    public byte[] write(BinaryWriter writer, HandshakeChallenge payload) {
        StarByteArrayCodec.INSTANCE.write(writer, payload.salt());
        return finish(writer);
    }
}
