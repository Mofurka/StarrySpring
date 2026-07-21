package irden.space.proxy.plugin.command_handler;

import irden.space.proxy.plugin.api.PacketInterceptionContext;
import irden.space.proxy.plugin.api.Permission;
import irden.space.proxy.plugin.api.PermissionView;
import irden.space.proxy.plugin.api.PluginSessionContext;
import irden.space.proxy.protocol.packet.PacketType;
import irden.space.proxy.protocol.payload.packet.chat.ChatReceive;
import org.intellij.lang.annotations.PrintFormat;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public record CommandContext(PacketInterceptionContext packetContext, String commandName, String rawInput,
                             String argumentsLine, List<String> rawArguments, Map<String, Object> arguments,
                             Map<CommandContextKey<?>, Object> values) {

    public CommandContext(
            PacketInterceptionContext packetContext,
            String commandName,
            String rawInput,
            String argumentsLine,
            List<String> rawArguments,
            Map<String, Object> arguments
    ) {
        this(packetContext, commandName, rawInput, argumentsLine, rawArguments, arguments, Map.of());
    }

    public CommandContext(
            PacketInterceptionContext packetContext,
            String commandName,
            String rawInput,
            String argumentsLine,
            List<String> rawArguments,
            Map<String, Object> arguments,
            Map<CommandContextKey<?>, Object> values
    ) {
        this.packetContext = Objects.requireNonNull(packetContext, "packetContext");
        this.commandName = Objects.requireNonNull(commandName, "commandName");
        this.rawInput = Objects.requireNonNull(rawInput, "rawInput");
        this.argumentsLine = Objects.requireNonNull(argumentsLine, "argumentsLine");
        this.rawArguments = List.copyOf(rawArguments);
        this.arguments = Collections.unmodifiableMap(new LinkedHashMap<>(arguments));
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(CommandArgumentContext argumentContext) {
        return builder()
                .packetContext(argumentContext.packetContext())
                .commandName(argumentContext.commandName())
                .rawInput(argumentContext.rawInput())
                .argumentsLine(argumentContext.argumentsLine())
                .rawArguments(argumentContext.rawArguments())
                .arguments(argumentContext.arguments());
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

    public boolean has(CommandContextKey<?> key) {
        return values.containsKey(key);
    }

    public <T> T get(String name, Class<T> type) {
        Object value = arguments.get(name);
        if (value == null) {
            throw new IllegalArgumentException("Missing command argument: " + name);
        }
        return type.cast(value);
    }

    public <T> T getOrNull(String name, Class<T> type) {
        Object value = arguments.get(name);
        if (value == null) {
            return null;
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

    public <T> Optional<T> get(CommandContextKey<T> key) {
        Object value = values.get(key);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(key.type().cast(value));
    }

    public <T> T getOrNull(CommandContextKey<T> key) {
        return get(key).orElse(null);
    }

    public <T> T getOrDefault(CommandContextKey<T> key, T defaultValue) {
        return get(key).orElse(defaultValue);
    }

    public <T> Optional<T> sender(Class<T> type) {
        Objects.requireNonNull(type, "type");

        for (Object value : values.values()) {
            if (type.isInstance(value)) {
                return Optional.of(type.cast(value));
            }
        }

        if (type.isInstance(session())) {
            return Optional.of(type.cast(session()));
        }

        return Optional.empty();
    }

    public void reply(@PrintFormat String formatMessage, @NotNull Object... args){
        reply(String.format(formatMessage, args));
    }

    public void reply(String message) {
        session().sendToClient(PacketType.CHAT_RECEIVE, CommandMessages.systemMessage(message));
    }

    public void reply(ChatReceive  message) {
        session().sendToClient(PacketType.CHAT_RECEIVE, message);
    }

    public static final class Builder {

        private PacketInterceptionContext packetContext;
        private String commandName;
        private String rawInput = "";
        private String argumentsLine = "";
        private List<String> rawArguments = List.of();
        private final Map<String, Object> arguments = new LinkedHashMap<>();
        private final Map<CommandContextKey<?>, Object> values = new LinkedHashMap<>();

        private Builder() {
        }

        public Builder packetContext(PacketInterceptionContext packetContext) {
            this.packetContext = Objects.requireNonNull(packetContext, "packetContext");
            return this;
        }

        public Builder commandName(String commandName) {
            this.commandName = Objects.requireNonNull(commandName, "commandName");
            return this;
        }

        public Builder rawInput(String rawInput) {
            this.rawInput = Objects.requireNonNull(rawInput, "rawInput");
            return this;
        }

        public Builder argumentsLine(String argumentsLine) {
            this.argumentsLine = Objects.requireNonNull(argumentsLine, "argumentsLine");
            return this;
        }

        public Builder rawArguments(List<String> rawArguments) {
            this.rawArguments = List.copyOf(rawArguments);
            return this;
        }

        public Builder arguments(Map<String, Object> arguments) {
            this.arguments.clear();
            if (arguments != null) {
                this.arguments.putAll(arguments);
            }
            return this;
        }

        public PluginSessionContext session() {
            return packetContext().session();
        }

        public PacketInterceptionContext packetContext() {
            return Objects.requireNonNull(packetContext, "packetContext");
        }

        public String commandName() {
            return Objects.requireNonNull(commandName, "commandName");
        }

        public String rawInput() {
            return rawInput;
        }

        public String argumentsLine() {
            return argumentsLine;
        }

        public List<String> rawArguments() {
            return List.copyOf(rawArguments);
        }

        public Map<String, Object> arguments() {
            return Collections.unmodifiableMap(arguments);
        }

        public Map<CommandContextKey<?>, Object> values() {
            return Collections.unmodifiableMap(values);
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

        public <T> T getOrNull(String name, Class<T> type) {
            Object value = arguments.get(name);
            if (value == null) {
                return null;
            }
            return type.cast(value);
        }

        public <T> Builder put(CommandContextKey<T> key, T value) {
            Objects.requireNonNull(key, "key");

            if (value == null) {
                values.remove(key);
                return this;
            }

            values.put(key, key.type().cast(value));
            return this;
        }

        public <T> Optional<T> get(CommandContextKey<T> key) {
            Object value = values.get(key);
            if (value == null) {
                return Optional.empty();
            }
            return Optional.of(key.type().cast(value));
        }

        public CommandContext build() {
            return new CommandContext(packetContext(), commandName(), rawInput, argumentsLine, rawArguments, arguments, values);
        }
    }
}