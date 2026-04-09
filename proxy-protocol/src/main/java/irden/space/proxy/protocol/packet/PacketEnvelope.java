package irden.space.proxy.protocol.packet;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import javax.annotation.processing.Generated;
import java.util.Objects;

@Getter
@EqualsAndHashCode
public final class PacketEnvelope {
    private final int packetType;
    private final int payloadSize;
    private final boolean compressed;
    private final byte[] payload;
    private final byte[] originalData;
    private final PacketDirection direction;

    public PacketEnvelope(int packetType, int payloadSize, boolean compressed, byte[] payload, byte[] originalData, PacketDirection direction) {
        this.packetType = packetType;
        this.payloadSize = payloadSize;
        this.compressed = compressed;
        this.payload = Objects.requireNonNull(payload, "Payload cannot be null");
        this.originalData = Objects.requireNonNull(originalData, "Original data cannot be null");
        this.direction = Objects.requireNonNull(direction, "Packet direction cannot be null");
    }


}
