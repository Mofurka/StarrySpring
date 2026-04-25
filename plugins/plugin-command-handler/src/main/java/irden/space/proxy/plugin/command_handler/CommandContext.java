package irden.space.proxy.plugin.command_handler;

import irden.space.proxy.plugin.api.PacketInterceptionContext;
import irden.space.proxy.plugin.api.PluginSessionContext;
import irden.space.proxy.protocol.packet.PacketType;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class CommandContext {

    private final PacketInterceptionContext packetContext;
    private final String commandName;
    private final String rawInput;
    private final String argumentsLine;
    private final List<String> rawArguments;
    private final Map<String, Object> arguments;

    public CommandContext(
            PacketInterceptionContext packetContext,
            String commandName,
            String rawInput,
            String argumentsLine,
            List<String> rawArguments,
            Map<String, Object> arguments
    ) {
        this.packetContext = Objects.requireNonNull(packetContext, "packetContext");
        this.commandName = Objects.requireNonNull(commandName, "commandName");
        this.rawInput = Objects.requireNonNull(rawInput, "rawInput");
        this.argumentsLine = Objects.requireNonNull(argumentsLine, "argumentsLine");
        this.rawArguments = List.copyOf(rawArguments);
        this.arguments = Map.copyOf(arguments);
    }

    public PacketInterceptionContext packetContext() {
        return packetContext;
    }

    public PluginSessionContext session() {
        return packetContext.session();
    }

    public String commandName() {
        return commandName;
    }

    public String rawInput() {
        return rawInput;
    }

    public String argumentsLine() {
        return argumentsLine;
    }

    public List<String> rawArguments() {
        return rawArguments;
    }

    public Map<String, Object> arguments() {
        return arguments;
    }

    public boolean has(String name) {
        return arguments.containsKey(name);
    }

    public <T> T get(String name, Class<T> type) {
        Object value = arguments.get(name);

        if (value == null) {
            throw new IllegalArgumentException("Missing command argument: " + name);
        }

        return type.cast(value);
    }

    public <T> T getOrDefault(String name, Class<T> type, T defaultValue) {
        Object value = arguments.get(name);

        if (value == null) {
            return defaultValue;
        }

        return type.cast(value);
    }

    public void reply(String message) {
        session().sendToClient(PacketType.CHAT_RECEIVE, CommandMessages.systemMessage(message));
    }
}