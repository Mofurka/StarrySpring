package irden.space.proxy.protocol.packet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class PacketTypeTest {

    @Test
    void resolvesEveryDeclaredIdBackToItsConstant() {
        for (PacketType type : PacketType.values()) {
            assertSame(type, PacketType.fromId(type.id()), "id " + type.id());
        }
    }

    @Test
    void returnsNullForIdsOutsideTheTable() {
        assertNull(PacketType.fromId(-1));
        assertNull(PacketType.fromId(Integer.MIN_VALUE));
        assertNull(PacketType.fromId(Integer.MAX_VALUE));
        assertNull(PacketType.fromId(PacketType.values().length + 1000));
    }
}
