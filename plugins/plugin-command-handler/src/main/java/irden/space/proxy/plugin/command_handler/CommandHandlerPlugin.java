package irden.space.proxy.plugin.command_handler;

import irden.space.proxy.plugin.api.*;
import irden.space.proxy.plugin.api.annotations.OnLoad;
import irden.space.proxy.plugin.api.annotations.PacketHandler;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketType;
import irden.space.proxy.protocol.payload.packet.chat.ChatSent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;

@PluginDefinition(
        id = "command-handler",
        name = "Command Handler",
        version = "1.0.0",
        author = "https://github.com/Mofurka",
        description = "A plugin that allows you to handle commands sent by clients."
)
public class CommandHandlerPlugin implements ProxyPlugin {

    private static final String COMMAND_PREFIX = "/";
    private static final Logger log = LoggerFactory.getLogger(CommandHandlerPlugin.class);

    private final CommandParser commandParser = new CommandParser();

    @OnLoad
    public void handleLoad(PluginContext context) {
        context.publishService(CommandHandlerPlugin.class, this);
    }

    @PacketHandler(value = PacketType.CHAT_SENT, direction = PacketDirection.TO_SERVER)
    public PacketDecision onChatSent(PacketInterceptionContext context) {
        ChatSent chatSent = (ChatSent) context.parsedPayload();
        String content = chatSent.content();

        if (content.isBlank() || !content.startsWith(COMMAND_PREFIX)) {
            return PacketDecision.forward();
        }

        String commandLine = content.substring(COMMAND_PREFIX.length()).trim();

        if (commandLine.isBlank()) {
            return PacketDecision.forward();
        }

        ParsedCommand parsedCommand = parse(commandLine);
        RegisteredCommand registeredCommand = CommandRegistry.global().find(parsedCommand.commandName());

        if (registeredCommand == null) {
            return PacketDecision.forward();
        }

        log.info(
                "Executing command '/{}' from plugin '{}'",
                registeredCommand.name(),
                registeredCommand.ownerPluginId()
        );

        CommandParseResult parseResult = commandParser.parse(
                registeredCommand.root(),
                parsedCommand.arguments()
        );

        if (parseResult instanceof CommandParseResult.Error error) {
            context.session().sendToClient(
                    PacketType.CHAT_RECEIVE,
                    CommandMessages.systemMessage(error.message())
            );
            return PacketDecision.cancel();
        }

        CommandParseResult.Success success = (CommandParseResult.Success) parseResult;

        CommandContext commandContext = new CommandContext(
                context,
                registeredCommand.name(),
                commandLine,
                parsedCommand.argumentsLine(),
                parsedCommand.arguments(),
                success.arguments()
        );

        try {
            success.executor().execute(commandContext);
            return PacketDecision.cancel();
        } catch (RuntimeException e) {
            log.error("Failed to execute command '/{}'", registeredCommand.name(), e);

            context.session().sendToClient(
                    PacketType.CHAT_RECEIVE,
                    CommandMessages.systemMessage("Command '/" + registeredCommand.name() + "' failed: " + e.getMessage())
            );

            return PacketDecision.cancel();
        }
    }

    private ParsedCommand parse(String commandLine) {
        String[] parts = commandLine.split("\\s+", 2);
        String commandName = parts[0].trim().toLowerCase(Locale.ROOT);
        String argumentsLine = parts.length > 1 ? parts[1].trim() : "";

        List<String> arguments = argumentsLine.isBlank()
                ? List.of()
                : List.of(argumentsLine.split("\\s+"));

        return new ParsedCommand(commandName, argumentsLine, arguments);
    }

    private record ParsedCommand(
            String commandName,
            String argumentsLine,
            List<String> arguments
    ) {
    }
}