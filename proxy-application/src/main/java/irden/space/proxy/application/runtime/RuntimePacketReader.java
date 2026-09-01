package irden.space.proxy.application.runtime;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.VlqCodec;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketEnvelope;
import irden.space.proxy.protocol.packet.PacketType;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Objects;

public class RuntimePacketReader {


    public static final int DEFAULT_MAX_PAYLOAD_SIZE_BYTES = 32 * 1024 * 1024;

    private static final long PACKET_STALL_TIMEOUT_MILLIS = 60_000L;

    private final PayloadCompressionCodec payloadCompressionCodec;
    private final int maxPayloadSizeBytes;

    public RuntimePacketReader(PayloadCompressionCodec payloadCompressionCodec) {
        this(payloadCompressionCodec, DEFAULT_MAX_PAYLOAD_SIZE_BYTES);
    }

    public RuntimePacketReader(PayloadCompressionCodec payloadCompressionCodec, int maxPayloadSizeBytes) {
        this.payloadCompressionCodec = Objects.requireNonNull(
                payloadCompressionCodec,
                "Payload compression codec cannot be null"
        );
        if (maxPayloadSizeBytes <= 0) {
            throw new IllegalArgumentException("Max payload size must be positive: " + maxPayloadSizeBytes);
        }
        this.maxPayloadSizeBytes = maxPayloadSizeBytes;
    }

    public PacketEnvelope read(InputStream inputStream, PacketDirection direction) throws IOException {
        int rawTypeId = inputStream.read();
        if (rawTypeId < 0) {
            throw new IOException("Stream closed while reading packet type");
        }

        SignedVlqReadResult sizeResult = readSignedVlqWithRawBytes(inputStream);

        int encodedSize = sizeResult.value();
        boolean compressed = encodedSize < 0;
        int payloadSize = requireAcceptablePayloadSize(encodedSize, rawTypeId);

        byte[] rawPayload = readPayload(inputStream, payloadSize);

        PacketType packetType = PacketType.fromId(rawTypeId);

        byte[] originalData = buildOriginal(rawTypeId, sizeResult.rawBytes(), rawPayload);
        byte[] payload = rawPayload;
        if (compressed) {
            payload = payloadCompressionCodec.decompress(rawPayload);
        }

        return new PacketEnvelope(
                rawTypeId,
                packetType,
                payloadSize,
                compressed,
                payload,
                originalData,
                direction
        );
    }


    private int requireAcceptablePayloadSize(int encodedSize, int rawTypeId) throws IOException {
        if (encodedSize == Integer.MIN_VALUE) {
            throw new IOException(
                    "Declared payload size " + encodedSize + " for packet type " + rawTypeId + " is not representable"
            );
        }

        int payloadSize = Math.abs(encodedSize);
        if (payloadSize > maxPayloadSizeBytes) {
            throw new IOException(
                    "Declared payload size " + payloadSize + " bytes for packet type " + rawTypeId
                            + " exceeds the limit of " + maxPayloadSizeBytes + " bytes"
            );
        }

        return payloadSize;
    }

    private byte[] buildOriginal(int rawTypeId, byte[] rawSizeBytes, byte[] payload) {
        byte[] original = new byte[1 + rawSizeBytes.length + payload.length];
        original[0] = (byte) rawTypeId;
        System.arraycopy(rawSizeBytes, 0, original, 1, rawSizeBytes.length);
        System.arraycopy(payload, 0, original, 1 + rawSizeBytes.length, payload.length);
        return original;
    }

    private int readWithinPacket(InputStream inputStream) throws IOException {
        long deadline = System.currentTimeMillis() + PACKET_STALL_TIMEOUT_MILLIS;

        while (true) {
            try {
                return inputStream.read();
            } catch (SocketTimeoutException e) {
                requirePacketNotStalled(deadline, e);
            }
        }
    }

    private byte[] readPayload(InputStream inputStream, int payloadSize) throws IOException {
        byte[] payload = new byte[payloadSize];
        long deadline = System.currentTimeMillis() + PACKET_STALL_TIMEOUT_MILLIS;
        int offset = 0;

        while (offset < payloadSize) {
            int read;
            try {
                read = inputStream.read(payload, offset, payloadSize - offset);
            } catch (SocketTimeoutException e) {
                requirePacketNotStalled(deadline, e);
                continue;
            }

            if (read < 0) {
                throw new IOException("Unexpected end of stream while reading payload");
            }
            offset += read;
        }

        return payload;
    }

    private void requirePacketNotStalled(long deadline, SocketTimeoutException timeout) throws IOException {
        if (System.currentTimeMillis() >= deadline) {
            throw new IOException(
                    "Packet stalled for more than " + PACKET_STALL_TIMEOUT_MILLIS + " ms",
                    timeout
            );
        }
    }

    private SignedVlqReadResult readSignedVlqWithRawBytes(InputStream inputStream) throws IOException {
        byte[] rawBytesBuffer = new byte[5];
        int length = 0;

        while (true) {
            if (length >= rawBytesBuffer.length) {
                throw new IOException("Signed VLQ exceeds expected int size");
            }

            int b = readWithinPacket(inputStream);
            if (b < 0) {
                throw new IOException("Stream closed while reading signed VLQ");
            }

            rawBytesBuffer[length++] = (byte) b;

            if ((b & 0x80) == 0) {
                break;
            }
        }

        byte[] rawBytes = Arrays.copyOf(rawBytesBuffer, length);
        BinaryReader reader = new BinaryReader(rawBytes);
        int value = VlqCodec.INSTANCE.read(reader);

        return new SignedVlqReadResult(value, rawBytes);
    }

    private record SignedVlqReadResult(int value, byte[] rawBytes) {
    }
}
