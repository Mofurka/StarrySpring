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
    public static PacketEnvelope of(int rawPacketTypeId, PacketType packetType, int payloadSize, boolean compressed, byte[] payload, byte[] originalData, PacketDirection direction) {
        return new PacketEnvelope(rawPacketTypeId, packetType, payloadSize, compressed, payload, originalData, direction);
    }
    public static PacketEnvelope of(int rawPacketTypeId, PacketType packetType, int payloadSize, boolean compressed, byte[] payload, byte[] originalData) {
        return new PacketEnvelope(rawPacketTypeId, packetType, payloadSize, compressed, payload, originalData, null);
    }

}
