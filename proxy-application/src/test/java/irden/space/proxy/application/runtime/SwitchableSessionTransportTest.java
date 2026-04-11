package irden.space.proxy.application.runtime;

import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketEnvelope;
import irden.space.proxy.protocol.packet.PacketType;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DeflaterOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwitchableSessionTransportTest {

    @Test
    void readsPacketFromZstdCompressedTransportStream() throws IOException {
        byte[] uncompressedPayload = "zstd-transport-payload".getBytes(StandardCharsets.UTF_8);
        byte[] zlibCompressedPayload = compressZlib(uncompressedPayload);
        byte[] packet = buildPacket(0, -zlibCompressedPayload.length, zlibCompressedPayload);
        byte[] zstdFrame = compressZstd(packet);

        SwitchableSessionTransport transport = new SwitchableSessionTransport(new PlainSessionTransport());
        transport.enableZstdRead();

        PacketEnvelope envelope = transport.read(new ByteArrayInputStream(zstdFrame), PacketDirection.TO_SERVER);

        assertTrue(transport.isZstd());
        assertEquals(0, envelope.rawPacketTypeId());
        assertEquals(PacketType.PROTOCOL_REQUEST, envelope.packetType());
        assertTrue(envelope.compressed());
        assertArrayEquals(uncompressedPayload, envelope.payload());
        assertArrayEquals(packet, envelope.originalData());
    }

    @Test
    void writesFirstPacketPlainAndCompressesFollowingPacketsAfterZstdSwitch() throws IOException {
        byte[] firstPacket = buildPacket(1, 4, new byte[]{10, 20, 30, 40});
        byte[] secondPacket = buildPacket(0, 3, "abc".getBytes(StandardCharsets.UTF_8));

        SwitchableSessionTransport transport = new SwitchableSessionTransport(new PlainSessionTransport());
        transport.enableZstdWrite(1);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        transport.write(outputStream, envelope(firstPacket, PacketType.PROTOCOL_RESPONSE));
        transport.write(outputStream, envelope(secondPacket, PacketType.PROTOCOL_REQUEST));

        byte[] writtenBytes = outputStream.toByteArray();
        byte[] secondFrameBytes = slice(writtenBytes, firstPacket.length, writtenBytes.length - firstPacket.length);

        assertArrayEquals(firstPacket, slice(writtenBytes, 0, firstPacket.length));
        assertArrayEquals(secondPacket, decompressZstd(secondFrameBytes));
    }

    private PacketEnvelope envelope(byte[] originalData, PacketType packetType) {
        return new PacketEnvelope(
                originalData[0] & 0xFF,
                packetType,
                originalData.length - 2,
                false,
                new byte[0],
                originalData,
                PacketDirection.TO_SERVER
        );
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

    private byte[] compressZstd(byte[] payload) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ZstdOutputStream zstdOutputStream = new ZstdOutputStream(outputStream)) {
            zstdOutputStream.write(payload);
        }
        return outputStream.toByteArray();
    }

    private byte[] decompressZstd(byte[] payload) throws IOException {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(payload);
             ZstdInputStream zstdInputStream = new ZstdInputStream(inputStream)) {
            return zstdInputStream.readAllBytes();
        }
    }

    private byte[] compressZlib(byte[] payload) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(outputStream)) {
            deflaterOutputStream.write(payload);
        }
        return outputStream.toByteArray();
    }

    private byte[] slice(byte[] source, int offset, int length) {
        byte[] result = new byte[length];
        System.arraycopy(source, offset, result, 0, length);
        return result;
    }

}
