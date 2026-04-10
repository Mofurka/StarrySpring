package irden.space.proxy.application.runtime;

import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketEnvelope;
import irden.space.proxy.protocol.packet.PacketType;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.zip.DeflaterOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class RuntimePacketReaderTest {

    @Test
    void readsLargeCompressedPacketWithoutSplittingFollowingPackets() throws IOException {
        byte[] uncompressedPayload = new byte[512];
        new Random(123456789L).nextBytes(uncompressedPayload);

        byte[] compressedPayload = compressZlib(uncompressedPayload);
        assertTrue(compressedPayload.length > 127, "compressed payload must require multi-byte signed VLQ");

        byte[] firstPacket = buildPacket(13, -compressedPayload.length, compressedPayload);
        byte[] secondPayload = new byte[]{1, 2, 3, 4};
        byte[] secondPacket = buildPacket(0, secondPayload.length, secondPayload);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(concat(firstPacket, secondPacket));
        RuntimePacketReader packetReader = new RuntimePacketReader();

        PacketEnvelope firstEnvelope = packetReader.read(inputStream, PacketDirection.TO_SERVER);
        assertEquals(13, firstEnvelope.rawPacketTypeId());
        assertEquals(PacketType.CLIENT_CONNECT, firstEnvelope.packetType());
        assertTrue(firstEnvelope.compressed());
        assertEquals(compressedPayload.length, firstEnvelope.payloadSize());
        assertArrayEquals(uncompressedPayload, firstEnvelope.payload());
        assertArrayEquals(firstPacket, firstEnvelope.originalData());

        PacketEnvelope secondEnvelope = packetReader.read(inputStream, PacketDirection.TO_SERVER);
        assertEquals(0, secondEnvelope.rawPacketTypeId());
        assertEquals(PacketType.PROTOCOL_REQUEST, secondEnvelope.packetType());
        assertEquals(secondPayload.length, secondEnvelope.payloadSize());
        assertArrayEquals(secondPayload, secondEnvelope.payload());
    }

    private byte[] buildPacket(int typeId, int signedSize, byte[] payload) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(typeId);
        outputStream.write(encodeSignedVlq(signedSize));
        outputStream.write(payload);
        return outputStream.toByteArray();
    }

    private byte[] encodeSignedVlq(int value) {
        int encoded = (value << 1) ^ (value >> 31);
        return encodeVlq(encoded);
    }

    private byte[] encodeVlq(int value) {
        if (value == 0) {
            return new byte[]{0};
        }

        List<Integer> groups = new ArrayList<>();
        int current = value;
        while (current > 0) {
            groups.add(current & 0x7F);
            current >>>= 7;
        }

        byte[] result = new byte[groups.size()];
        for (int i = groups.size() - 1, j = 0; i >= 0; i--, j++) {
            int group = groups.get(i);
            if (i != 0) {
                group |= 0x80;
            }
            result[j] = (byte) group;
        }
        return result;
    }

    private byte[] compressZlib(byte[] payload) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(outputStream)) {
            deflaterOutputStream.write(payload);
        }
        return outputStream.toByteArray();
    }

    private byte[] concat(byte[] first, byte[] second) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(first);
        outputStream.write(second);
        return outputStream.toByteArray();
    }
}

