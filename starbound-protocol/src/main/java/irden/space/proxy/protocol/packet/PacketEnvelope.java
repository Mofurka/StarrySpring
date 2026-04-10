package irden.space.proxy.protocol.packet;

public record PacketEnvelope(
        int rawPacketTypeId,
        PacketType packetType,
        int payloadSize,
        boolean compressed,
        byte[] payload,
        byte[] originalData,
        PacketDirection direction
) {
}
