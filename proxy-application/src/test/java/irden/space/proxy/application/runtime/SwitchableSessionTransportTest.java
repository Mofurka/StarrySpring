package irden.space.proxy.application.runtime;

import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;
import irden.space.proxy.domain.session.SessionTransportMode;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketEnvelope;
import irden.space.proxy.protocol.packet.PacketType;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.DeflaterOutputStream;

import static org.junit.jupiter.api.Assertions.*;

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


    @Test
    void writesSelfContainedFramesReadableBySingleZstdStream() throws IOException {
        byte[] firstPacket = buildPacket(1, 4, new byte[]{10, 20, 30, 40});
        byte[] secondPacket = buildPacket(0, 3, "abc".getBytes(StandardCharsets.UTF_8));
        byte[] thirdPacket = buildPacket(6, 5, "hello".getBytes(StandardCharsets.UTF_8));

        SwitchableSessionTransport transport = new SwitchableSessionTransport(new PlainSessionTransport());
        transport.enableZstdWrite(0);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        transport.write(outputStream, envelope(firstPacket, PacketType.PROTOCOL_RESPONSE));
        transport.write(outputStream, envelope(secondPacket, PacketType.PROTOCOL_REQUEST));
        transport.write(outputStream, envelope(thirdPacket, PacketType.CHAT_RECEIVE));

        byte[] decoded;
        try (ZstdInputStream zstdInputStream =
                     new ZstdInputStream(new ByteArrayInputStream(outputStream.toByteArray()))) {
            decoded = zstdInputStream.readAllBytes();
        }

        ByteArrayOutputStream expected = new ByteArrayOutputStream();
        expected.write(firstPacket);
        expected.write(secondPacket);
        expected.write(thirdPacket);

        assertArrayEquals(expected.toByteArray(), decoded);
    }


    @Test
    void appliesRequestedReadModeOnlyAtPacketBoundary() throws IOException, InterruptedException {
        InputStream source = plainThenZstdStream();
        SwitchableSessionTransport transport = new SwitchableSessionTransport(new PlainSessionTransport());

        PacketEnvelope plainPacket = transport.read(source, PacketDirection.TO_SERVER);
        assertEquals(PacketType.PROTOCOL_REQUEST, plainPacket.packetType());

        transport.enableReadMode(SessionTransportMode.ZSTD);

        assertTrue(transport.isReadModeEnabled(SessionTransportMode.ZSTD), "режим должен быть запрошен");
        assertFalse(transport.isReadModeApplied(SessionTransportMode.ZSTD), "но ещё не применён");
        assertFalse(transport.awaitReadModeApplied(SessionTransportMode.ZSTD, 20));

        PacketEnvelope compressedPacket = transport.read(source, PacketDirection.TO_SERVER);

        assertTrue(transport.isReadModeApplied(SessionTransportMode.ZSTD));
        assertEquals(PacketType.PROTOCOL_RESPONSE, compressedPacket.packetType());
    }


    @Test
    void awaitReadModeAppliedIsReleasedByTheReaderThread() throws Exception {
        InputStream source = plainThenZstdStream();
        SwitchableSessionTransport transport = new SwitchableSessionTransport(new PlainSessionTransport());
        transport.read(source, PacketDirection.TO_SERVER);

        transport.enableReadMode(SessionTransportMode.ZSTD);

        CountDownLatch started = new CountDownLatch(1);
        AtomicBoolean confirmed = new AtomicBoolean();
        Thread waiter = new Thread(() -> {
            started.countDown();
            try {
                confirmed.set(transport.awaitReadModeApplied(SessionTransportMode.ZSTD, 5_000));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "switch-waiter");
        waiter.start();

        assertTrue(started.await(5, TimeUnit.SECONDS));

        transport.read(source, PacketDirection.TO_SERVER);

        waiter.join(5_000);
        assertFalse(waiter.isAlive(), "ожидающий поток должен быть разбужен читателем");
        assertTrue(confirmed.get());
    }

    @Test
    void appliesReadModeImmediatelyWhenNoReaderIsAttachedYet() throws InterruptedException {
        SwitchableSessionTransport transport = new SwitchableSessionTransport(new PlainSessionTransport());

        transport.enableReadMode(SessionTransportMode.ZSTD);

        assertTrue(transport.isReadModeApplied(SessionTransportMode.ZSTD));
        assertTrue(transport.awaitReadModeApplied(SessionTransportMode.ZSTD, 0));
    }

    private InputStream plainThenZstdStream() throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write(buildPacket(0, 3, "abc".getBytes(StandardCharsets.UTF_8)));
        stream.write(compressZstd(buildPacket(1, 4, new byte[]{9, 8, 7, 6})));
        return new ByteArrayInputStream(stream.toByteArray());
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
