package irden.space.proxy.application.runtime;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.SignedVlqCodec;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketEnvelope;
import irden.space.proxy.protocol.packet.PacketType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.InflaterInputStream;

public class RuntimePacketReader {

    public PacketEnvelope read(InputStream inputStream, PacketDirection direction) throws IOException {
        int rawTypeId = inputStream.read();
        if (rawTypeId < 0) {
            throw new IOException("Stream closed while reading packet type");
        }

        SignedVlqReadResult sizeResult = readSignedVlqWithRawBytes(inputStream);

        int encodedSize = sizeResult.value();
        boolean compressed = encodedSize < 0;
        int payloadSize = Math.abs(encodedSize);

        byte[] rawPayload = inputStream.readNBytes(payloadSize);
        if (rawPayload.length != payloadSize) {
            throw new IOException("Unexpected end of stream while reading payload");
        }

        PacketType packetType = PacketType.fromId(rawTypeId);

        byte[] originalData = buildOriginal(rawTypeId, sizeResult.rawBytes(), rawPayload);
        byte[] payload = compressed ? decompressZlib(rawPayload) : rawPayload;

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

    private byte[] buildOriginal(int rawTypeId, byte[] rawSizeBytes, byte[] payload) {
        BinaryWriter writer = new BinaryWriter();
        writer.writeByte(rawTypeId);
        writer.writeBytes(rawSizeBytes);
        writer.writeBytes(payload);
        return writer.toByteArray();
    }

    private byte[] decompressZlib(byte[] payload) throws IOException {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(payload);
             InflaterInputStream inflaterInputStream = new InflaterInputStream(byteArrayInputStream)) {
            return inflaterInputStream.readAllBytes();
        } catch (IOException e) {
            throw new IOException("Failed to decompress zlib payload", e);
        }
    }

    private SignedVlqReadResult readSignedVlqWithRawBytes(InputStream inputStream) throws IOException {
        BinaryWriter rawWriter = new BinaryWriter();

        while (true) {
            int b = inputStream.read();
            if (b < 0) {
                throw new IOException("Stream closed while reading signed VLQ");
            }

            rawWriter.writeByte(b);

            if ((b & 0x80) == 0) {
                break;
            }
        }

        byte[] rawBytes = rawWriter.toByteArray();
        BinaryReader reader = new BinaryReader(rawBytes);
        int value = SignedVlqCodec.read(reader);

        return new SignedVlqReadResult(value, rawBytes);
    }

    private record SignedVlqReadResult(int value, byte[] rawBytes) {
    }
}
