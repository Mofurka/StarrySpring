package irden.space.proxy.plugin.command_handler;

import irden.space.proxy.plugin.api.PacketInterceptionContext;
import irden.space.proxy.plugin.api.PluginSessionContext;
import irden.space.proxy.protocol.packet.PacketDirection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static irden.space.proxy.plugin.command_handler.CommandSpec.argument;
import static irden.space.proxy.plugin.command_handler.CommandSpec.literal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class CommandParserTest {

    @Test
    void shouldPassParsingContextToArgumentTypes() {
        CommandSpec spec = literal("audit")
                .then(argument("target", StringArgumentType.word())
                        .then(argument("note", new ContextAwareArgumentType())
                                .executes(context -> {
                                })))
                .build();

        List<CommandToken> tokens = CommandTokenizer.tokenize("Alice warning");
        CommandArgumentContext argumentContext = new CommandArgumentContext(
                packetContext("session-42"),
                "audit",
                "audit Alice warning",
                "Alice warning",
                List.of("Alice", "warning"),
                Map.of()
        );

        CommandParseResult result = new CommandParser().parse(spec.root(), argumentContext, tokens);
        CommandParseResult.Success success = assertInstanceOf(CommandParseResult.Success.class, result);

        assertEquals("session-42:Alice:warning", success.arguments().get("note"));
    }

    private static PacketInterceptionContext packetContext(String sessionId) {
        return new PacketInterceptionContext(new TestSessionContext(sessionId), null, null, PacketDirection.TO_SERVER);
    }

    private static final class ContextAwareArgumentType implements ArgumentType<String> {

        @Override
        public String parse(String input) {
            return input;
        }

        @Override
        public String parse(CommandArgumentContext context, String input) {
            return context.session().sessionId() + ":" + context.get("target", String.class) + ":" + input;
        }
    }

    private record TestSessionContext(String sessionId) implements PluginSessionContext {

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
    }
}

