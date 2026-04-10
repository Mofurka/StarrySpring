package irden.space.proxy.protocol.payload.packet.entity_message;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.variant.StringVariantValue;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class EntityMessageParserTest {

    private final EntityMessageParser parser = new EntityMessageParser();

    @Test
    void parsesAndWritesEntityIdTarget() {
        byte[] uuidBytes = new byte[]{
                0x00, 0x01, 0x02, 0x03,
                0x04, 0x05, 0x06, 0x07,
                0x08, 0x09, 0x0A, 0x0B,
                0x0C, 0x0D, 0x0E, 0x0F
        };
        byte[] payloadBytes = new byte[]{
                0x00,
                0x00, 0x01, (byte) 0xE2, 0x40,
                0x04, 'p', 'i', 'n', 'g',
                0x00,
                0x00, 0x01, 0x02, 0x03,
                0x04, 0x05, 0x06, 0x07,
                0x08, 0x09, 0x0A, 0x0B,
                0x0C, 0x0D, 0x0E, 0x0F,
                0x02, 0x01
        };

        EntityMessage message = parser.parse(new BinaryReader(payloadBytes));

        EntityIdTarget target = assertInstanceOf(EntityIdTarget.class, message.entityId());
        assertEquals(123456, target.entityId());
        assertEquals("ping", message.message());
        assertEquals(List.of(), message.args());
        assertEquals(new StarUuid(uuidBytes), message.uuid());
        assertEquals(513, message.fromConnection());
        assertArrayEquals(payloadBytes, parser.write(message));
    }

    @Test
    void parsesAndWritesUniqueEntityTarget() {
        byte[] uuidBytes = new byte[]{
                0x10, 0x11, 0x12, 0x13,
                0x14, 0x15, 0x16, 0x17,
                0x18, 0x19, 0x1A, 0x1B,
                0x1C, 0x1D, 0x1E, 0x1F
        };
        byte[] payloadBytes = new byte[]{
                0x01,
                0x04, 'd', 'o', 'o', 'r',
                0x04, 'o', 'p', 'e', 'n',
                0x01,
                0x05, 0x01, 'x',
                0x10, 0x11, 0x12, 0x13,
                0x14, 0x15, 0x16, 0x17,
                0x18, 0x19, 0x1A, 0x1B,
                0x1C, 0x1D, 0x1E, 0x1F,
                0x00, 0x07
        };

        EntityMessage message = parser.parse(new BinaryReader(payloadBytes));

        UniqueEntityIdTarget target = assertInstanceOf(UniqueEntityIdTarget.class, message.entityId());
        assertEquals("door", target.uniqueEntityId());
        assertEquals("open", message.message());
        assertEquals(List.of(new StringVariantValue("x")), message.args());
        assertEquals(new StarUuid(uuidBytes), message.uuid());
        assertEquals(7, message.fromConnection());
        assertArrayEquals(payloadBytes, parser.write(message));
    }
}

