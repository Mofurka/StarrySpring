package irden.space.proxy.protocol.payload.packet.handshake;

public record HandshakeResponse(
        byte[] passHash
) {
}
