package irden.space.proxy.plugin.command_handler;

import irden.space.proxy.plugin.api.PacketInterceptionContext;
import irden.space.proxy.plugin.api.PluginSessionContext;
import irden.space.proxy.protocol.packet.PacketDirection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandContextTest {

    private static final CommandContextKey<TestSender> TEST_SENDER_KEY =
            CommandContextKey.of("testSender", TestSender.class);

    @Test
    void shouldStoreResolvedValuesSeparatelyFromArguments() {
        CommandContext context = CommandContext.builder()
                .packetContext(packetContext("session-1"))
                .commandName("ban")
                .rawInput("ban Alice")
                .argumentsLine("Alice")
                .rawArguments(List.of("Alice"))
                .arguments(Map.of("target", "Alice"))
                .put(TEST_SENDER_KEY, new TestSender("Moderator"))
                .build();

        assertEquals("Alice", context.get("target", String.class));
        assertEquals("Moderator", context.get(TEST_SENDER_KEY).orElseThrow().name());
        assertEquals("Moderator", context.sender(TestSender.class).orElseThrow().name());
        assertTrue(context.has(TEST_SENDER_KEY));
    }

    private static PacketInterceptionContext packetContext(String sessionId) {
        return new PacketInterceptionContext(new TestSessionContext(sessionId), null, null, PacketDirection.TO_SERVER);
    }

    private record TestSender(String name) {
    }

    private record TestSessionContext(String sessionId) implements PluginSessionContext {

        @Override
        public String clientIp() {
            return "127.0.0.1";
        }

        @Override
        public Map<String, Object> attributes() {
            return Map.of();
        }

        @Override
        public boolean clientZstdEnabled() {
            return false;
        }

        @Override
        public boolean upstreamZstdEnabled() {
            return false;
        }
    }
}

