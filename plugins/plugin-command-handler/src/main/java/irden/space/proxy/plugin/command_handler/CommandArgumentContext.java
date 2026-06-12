package irden.space.proxy.plugin.command_handler;

import irden.space.proxy.plugin.api.PacketInterceptionContext;
import irden.space.proxy.plugin.api.PermissionView;
import irden.space.proxy.plugin.api.PluginSessionContext;

import java.util.*;

public record CommandArgumentContext(
        PacketInterceptionContext packetContext,
        String commandName,
        String rawInput,
        String argumentsLine,
        List<String> rawArguments,
        Map<String, Object> arguments
) {

    public CommandArgumentContext {
        packetContext = Objects.requireNonNull(packetContext, "packetContext");
        commandName = Objects.requireNonNull(commandName, "commandName");
        rawInput = Objects.requireNonNull(rawInput, "rawInput");
        argumentsLine = Objects.requireNonNull(argumentsLine, "argumentsLine");
        rawArguments = List.copyOf(rawArguments);
        arguments = Collections.unmodifiableMap(new LinkedHashMap<>(arguments));
    }

    public PluginSessionContext session() {
        return packetContext.session();
    }

    public PermissionView permissions() {
        return session().permissions();
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

    public CommandArgumentContext withArguments(Map<String, Object> arguments) {
        return new CommandArgumentContext(packetContext, commandName, rawInput, argumentsLine, rawArguments, arguments);
    }
}

