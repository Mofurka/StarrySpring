package irden.space.proxy.plugin.command_handler;

import irden.space.proxy.plugin.api.*;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketEnvelope;
import irden.space.proxy.protocol.packet.PacketType;
import irden.space.proxy.protocol.payload.packet.chat.ChatReceive;
import irden.space.proxy.protocol.payload.packet.chat.ChatSent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CommandHandlerPluginTest {

    private final PacketInterceptorRegistry interceptorRegistry = new PacketInterceptorRegistry() {
        private final List<PacketInterceptor> interceptors = new ArrayList<>();

        @Override
        public void register(PacketInterceptor interceptor) {
            interceptors.add(interceptor);
        }

        @Override
        public List<PacketInterceptor> getAll() {
            return List.copyOf(interceptors);
        }
    };

    private final PluginContext pluginContext = () -> interceptorRegistry;

    @BeforeEach
    @AfterEach
    void resetRegistry() {
        CommandRegistry.global().clear();
    }

    @Test
    void registersAnnotatedCommandsOnPluginLoad() {
        TestCommandPlugin plugin = new TestCommandPlugin();

        plugin.onLoad(pluginContext);

        RegisteredCommand registeredCommand = CommandRegistry.global().find("ping");
        assertNotNull(registeredCommand);
        assertEquals("test-plugin", registeredCommand.ownerPluginId());
        assertEquals(List.of("pong"), registeredCommand.aliases());
    }

    @Test
    void executesRegisteredCommandAndCancelsPacket() {
        CommandHandlerPlugin handlerPlugin = new CommandHandlerPlugin();
        handlerPlugin.onLoad(pluginContext);

        TestCommandPlugin commandPlugin = new TestCommandPlugin();
        commandPlugin.onLoad(pluginContext);

        RecordingSession session = new RecordingSession();
        PacketInterceptionContext context = new PacketInterceptionContext(
                session,
                chatEnvelope(),
                new ChatSent("/ping alpha beta", null, List.of()),
                PacketDirection.TO_SERVER
        );

        PacketDecision decision = handlerPlugin.onChatSent(context);

        assertTrue(decision.isDrop());
        assertEquals(1, session.clientPackets.size());
        ChatReceive chatReceive = (ChatReceive) session.clientPackets.getFirst().payload();
        assertEquals("pong: alpha beta", chatReceive.message());
    }

    @Test
    void forwardsUnknownSlashCommands() {
        CommandHandlerPlugin handlerPlugin = new CommandHandlerPlugin();
        handlerPlugin.onLoad(pluginContext);

        RecordingSession session = new RecordingSession();
        PacketInterceptionContext context = new PacketInterceptionContext(
                session,
                chatEnvelope(),
                new ChatSent("/unknown command", null, List.of()),
                PacketDirection.TO_SERVER
        );

        PacketDecision decision = handlerPlugin.onChatSent(context);

        assertTrue(decision.isForward());
        assertTrue(session.clientPackets.isEmpty());
        assertTrue(session.serverPackets.isEmpty());
    }

    @Test
    void rejectsDuplicateCommandLabels() {
        DuplicateCommandPlugin plugin = new DuplicateCommandPlugin();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> plugin.onLoad(pluginContext));

        assertTrue(error.getMessage().contains("already registered"));
    }

    private PacketEnvelope chatEnvelope() {
        return PacketEnvelope.of(0, PacketType.CHAT_SENT, 0, false, new byte[0], new byte[0], PacketDirection.TO_SERVER);
    }

    @PluginDefinition(id = "test-plugin", name = "Test Plugin", version = "1.0.0", dependsOn = {"command-handler"})
    private static final class TestCommandPlugin implements ProxyPlugin {

        @ChatCommand(value = "ping", aliases = {"pong"}, description = "Replies with pong", usage = "<text>")
        public void ping(CommandContext context) {
            context.reply("pong: " + context.argumentsLine());
        }
    }

    @PluginDefinition(id = "duplicate-plugin", name = "Duplicate Plugin", version = "1.0.0", dependsOn = {"command-handler"})
    private static final class DuplicateCommandPlugin implements ProxyPlugin {

        @ChatCommand("dup")
        public void first() {
        }

        @ChatCommand("dup")
        public void second() {
        }
    }

    private static final class RecordingSession implements PluginSessionContext {
        private final List<SentPacket> clientPackets = new ArrayList<>();
        private final List<SentPacket> serverPackets = new ArrayList<>();

        @Override
        public String sessionId() {
            return "test-session";
        }

        @Override
        public String clientIp() {
            return "127.0.0.1";
        }

        @Override
        public boolean clientZstdEnabled() {
            return false;
        }

        @Override
        public boolean upstreamZstdEnabled() {
            return false;
        }

        @Override
        public void send(PacketDirection direction, PacketType packetType, Object payload) {
            if (direction == PacketDirection.TO_CLIENT) {
                clientPackets.add(new SentPacket(packetType, payload));
                return;
            }

            serverPackets.add(new SentPacket(packetType, payload));
        }
    }

    private record SentPacket(PacketType packetType, Object payload) {
    }
}

