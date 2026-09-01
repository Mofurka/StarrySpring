package irden.space.proxy.application.runtime;

import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.VlqCodec;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketEnvelope;
import irden.space.proxy.protocol.packet.PacketType;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class RuntimePacketReaderTest {

    private static final int MAX_PAYLOAD_SIZE_BYTES = 1024;

    @Test
    void readsPacketWithinPayloadSizeLimit() throws IOException {
        byte[] payload = "chat".getBytes(StandardCharsets.UTF_8);
        byte[] packet = packet(PacketType.CHAT_SENT.id(), payload.length, payload);

        PacketEnvelope envelope = reader().read(new ByteArrayInputStream(packet), PacketDirection.TO_SERVER);

        assertEquals(PacketType.CHAT_SENT, envelope.packetType());
        assertArrayEquals(payload, envelope.payload());
    }


    @Test
    void rejectsDeclaredPayloadSizeAboveLimitBeforeAllocatingBuffer() {
        byte[] headerOnly = packet(PacketType.CHAT_SENT.id(), MAX_PAYLOAD_SIZE_BYTES + 1, new byte[0]);

        IOException failure = assertThrows(
                IOException.class,
                () -> reader().read(new ByteArrayInputStream(headerOnly), PacketDirection.TO_SERVER)
        );

        assertTrue(failure.getMessage().contains("exceeds the limit"), failure.getMessage());
    }

    @Test
    void rejectsCompressedPayloadSizeAboveLimit() {
        byte[] headerOnly = packet(PacketType.CHAT_SENT.id(), -(MAX_PAYLOAD_SIZE_BYTES + 1), new byte[0]);

        IOException failure = assertThrows(
                IOException.class,
                () -> reader().read(new ByteArrayInputStream(headerOnly), PacketDirection.TO_SERVER)
        );

        assertTrue(failure.getMessage().contains("exceeds the limit"), failure.getMessage());
    }


    @Test
    void rejectsPayloadSizeThatIsNotRepresentableAsPositiveInt() {
        byte[] headerOnly = {
                (byte) PacketType.CHAT_SENT.id(),
                (byte) 0x8F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x7F
        };

        IOException failure = assertThrows(
                IOException.class,
                () -> reader().read(new ByteArrayInputStream(headerOnly), PacketDirection.TO_SERVER)
        );

        assertTrue(failure.getMessage().contains("not representable"), failure.getMessage());
    }

    @Test
    void rejectsNonPositiveMaxPayloadSize() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new RuntimePacketReader(new ZlibPayloadCompressionCodec(), 0)
        );
    }

    private RuntimePacketReader reader() {
        return new RuntimePacketReader(new ZlibPayloadCompressionCodec(), MAX_PAYLOAD_SIZE_BYTES);
    }

    private byte[] packet(int typeId, int signedSize, byte[] payload) {
        BinaryWriter writer = new BinaryWriter();
        writer.writeByte(typeId);
        VlqCodec.INSTANCE.write(writer, signedSize);
        writer.writeBytes(payload);
        return writer.toByteArray();
    }
}
