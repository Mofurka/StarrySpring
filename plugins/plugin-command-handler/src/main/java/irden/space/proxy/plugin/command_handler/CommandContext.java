package irden.space.proxy.plugin.command_handler;

import irden.space.proxy.plugin.api.PacketInterceptionContext;
import irden.space.proxy.plugin.api.Permission;
import irden.space.proxy.plugin.api.PermissionView;
import irden.space.proxy.plugin.api.PluginSessionContext;
import irden.space.proxy.protocol.packet.PacketType;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record CommandContext(PacketInterceptionContext packetContext, String commandName, String rawInput,
                             String argumentsLine, List<String> rawArguments, Map<String, Object> arguments) {

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

    public PluginSessionContext session() {
        return packetContext.session();
    }

    public PermissionView permissions() {
        return session().permissions();
    }

    public boolean hasPermission(Permission permission) {
        return permissions().has(permission);
    }

    public boolean hasAllPermissions(Permission... permissions) {
        return permissions().hasAll(permissions);
    }

    public boolean hasAnyPermission(Permission... permissions) {
        return permissions().hasAny(permissions);
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