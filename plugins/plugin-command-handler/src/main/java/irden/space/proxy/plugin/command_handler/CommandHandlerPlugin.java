package irden.space.proxy.plugin.command_handler;

import irden.space.proxy.plugin.api.*;
import irden.space.proxy.plugin.api.annotations.PacketHandler;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketType;
import irden.space.proxy.protocol.payload.packet.chat.ChatSent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@PluginDefinition(
        id = "command-handler",
        name = "Command Handler",
        version = "1.0.0",
        author = "https://github.com/Mofurka",
        description = "A plugin that allows you to handle commands sent by clients."
)
@Component
public class CommandHandlerPlugin implements ProxyPlugin {

    private static final String COMMAND_PREFIX = "/";
    private static final Logger log = LoggerFactory.getLogger(CommandHandlerPlugin.class);

    private final CommandParser commandParser;
    private final List<CommandContextResolver> contextResolvers = new CopyOnWriteArrayList<>();

    public CommandHandlerPlugin(CommandParser commandParser) {
        this.commandParser = commandParser;
    }

    public void addContextResolver(CommandContextResolver resolver) {
        contextResolvers.add(Objects.requireNonNull(resolver, "resolver"));
    }

    public void removeContextResolver(CommandContextResolver resolver) {
        contextResolvers.remove(Objects.requireNonNull(resolver, "resolver"));
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
        List<String> rawArguments = parsedCommand.tokens()
                .stream()
                .map(CommandToken::value)
                .toList();

        if (registeredCommand == null) {
            return PacketDecision.forward();
        }

        CommandArgumentContext argumentContext = new CommandArgumentContext(
                context,
                registeredCommand.name(),
                commandLine,
                parsedCommand.argumentsLine(),
                rawArguments,
                Map.of()
        );

        log.info(
                "Executing command '/{}' from plugin '{}'",
                registeredCommand.name(),
                registeredCommand.ownerPluginId()
        );

        CommandParseResult parseResult = commandParser.parse(
                registeredCommand.root(),
                argumentContext,
                parsedCommand.tokens()
        );
        if (parseResult instanceof CommandParseResult.Error(String message)) {
            context.session().sendToClient(
                    PacketType.CHAT_RECEIVE,
                    CommandMessages.systemMessage(message)
            );
            return PacketDecision.cancel();
        }

        CommandParseResult.Success success = (CommandParseResult.Success) parseResult;

        if (!hasRequiredPermissions(context.session(), success.matchedNodes())) {
            context.session().sendToClient(
                    PacketType.CHAT_RECEIVE,
                    CommandMessages.systemMessage("You do not have permission to use this command.")
            );
            return PacketDecision.cancel();
        }

        try {
            CommandContext commandContext = createCommandContext(argumentContext, success.arguments());
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
        String argumentsLine = parts.length > 1 ? parts[1] : "";

        List<CommandToken> tokens = CommandTokenizer.tokenize(argumentsLine);

        return new ParsedCommand(commandName, argumentsLine, tokens);
    }

    private boolean hasRequiredPermissions(PluginSessionContext session, List<CommandNode> matchedNodes) {
        PermissionView permissions = session.permissions();

        for (CommandNode node : matchedNodes) {
            if (node.hasRequiredPermissions() && !permissions.hasAll(node.requiredPermissions())) {
                return false;
            }
        }

        return true;
    }

    public Collection<RegisteredCommand> allCommands() {
        return CommandRegistry.global().allCommands();
    }

    public List<String> autocomplete(
            PacketInterceptionContext packetContext,
            String commandName,
            List<String> literalPath,
            Map<String, String> optionValues,
            String focusedOptionName,
            String focusedValue
    ) {
        Objects.requireNonNull(packetContext, "packetContext");
        Objects.requireNonNull(commandName, "commandName");
        Objects.requireNonNull(literalPath, "literalPath");
        Objects.requireNonNull(optionValues, "optionValues");
        Objects.requireNonNull(focusedOptionName, "focusedOptionName");

        RegisteredCommand registeredCommand = CommandRegistry.global().find(commandName);
        if (registeredCommand == null) {
            return List.of();
        }

        CommandNode current = registeredCommand.root();
        for (String literalName : literalPath) {
            current = findLiteralChild(current, literalName);
            if (current == null) {
                return List.of();
            }
        }

        LinkedHashMap<String, Object> parsedArguments = new LinkedHashMap<>();
        LinkedHashMap<String, String> rawArguments = new LinkedHashMap<>();

        while (true) {
            List<ArgumentNode<?>> argumentChildren = argumentChildren(current);
            if (argumentChildren.isEmpty()) {
                return List.of();
            }

            if (argumentChildren.size() > 1) {
                if (log.isDebugEnabled()) {
                    log.debug("Autocomplete supports only linear argument chains near node '{}'", current.name());
                }
                return List.of();
            }

            ArgumentNode<?> argumentNode = argumentChildren.getFirst();

            if (matchesAutocompleteName(argumentNode.name(), focusedOptionName)) {
                CommandContext commandContext = createCommandContext(
                        createArgumentContext(packetContext, registeredCommand.name(), rawArguments, parsedArguments),
                        parsedArguments
                );
                return argumentNode.type().suggestions(commandContext, focusedValue == null ? "" : focusedValue);
            }

            String rawValue = findOptionValue(optionValues, argumentNode.name());
            if (rawValue == null) {
                if (argumentNode.required()) {
                    return List.of();
                }

                current = argumentNode;
                continue;
            }

            Object parsedValue;
            try {
                parsedValue = argumentNode.type().parse(
                        createArgumentContext(packetContext, registeredCommand.name(), rawArguments, parsedArguments),
                        rawValue
                );
            } catch (RuntimeException ex) {
                return List.of();
            }

            parsedArguments.put(argumentNode.name(), parsedValue);
            rawArguments.put(argumentNode.name(), rawValue);
            current = argumentNode;
        }
    }

    private CommandContext createCommandContext(CommandArgumentContext argumentContext, Map<String, Object> arguments) {
        CommandContext.Builder builder = CommandContext.builder(argumentContext)
                .arguments(arguments);

        for (CommandContextResolver resolver : contextResolvers) {
            resolver.resolve(builder);
        }

        return builder.build();
    }

    private CommandArgumentContext createArgumentContext(
            PacketInterceptionContext packetContext,
            String commandName,
            Map<String, String> rawArguments,
            Map<String, Object> parsedArguments
    ) {
        String argumentsLine = String.join(" ", rawArguments.values());
        String rawInput = argumentsLine.isBlank() ? commandName : commandName + " " + argumentsLine;

        return new CommandArgumentContext(
                packetContext,
                commandName,
                rawInput,
                argumentsLine,
                List.copyOf(rawArguments.values()),
                parsedArguments
        );
    }

    private CommandNode findLiteralChild(CommandNode current, String literalName) {
        for (CommandNode child : current.children()) {
            if (child instanceof LiteralNode literal && matchesAutocompleteName(literal.name(), literalName)) {
                return literal;
            }
        }

        return null;
    }

    private List<ArgumentNode<?>> argumentChildren(CommandNode current) {
        List<ArgumentNode<?>> result = new ArrayList<>();

        for (CommandNode child : current.children()) {
            if (child instanceof ArgumentNode<?> argumentNode) {
                result.add(argumentNode);
            }
        }

        return List.copyOf(result);
    }

    private String findOptionValue(Map<String, String> optionValues, String argumentName) {
        for (Map.Entry<String, String> entry : optionValues.entrySet()) {
            if (matchesAutocompleteName(argumentName, entry.getKey())) {
                return entry.getValue();
            }
        }

        return null;
    }

    private boolean matchesAutocompleteName(String expected, String actual) {
        return normalizeAutocompleteName(expected).equals(normalizeAutocompleteName(actual));
    }

    private String normalizeAutocompleteName(String value) {
        return value == null
                ? ""
                : value.trim().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private record ParsedCommand(
            String commandName,
            String argumentsLine,
            List<CommandToken> tokens
    ) {
    }
}
