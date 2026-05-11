package irden.space.proxy.plugin.command_handler;

import java.util.Objects;

public final class CommandContextKey<T> {

    private final String name;
    private final Class<T> type;

    private CommandContextKey(String name, Class<T> type) {
        this.name = normalizeName(name);
        this.type = Objects.requireNonNull(type, "type");
    }

    public static <T> CommandContextKey<T> of(String name, Class<T> type) {
        return new CommandContextKey<>(name, type);
    }

    public String name() {
        return name;
    }

    public Class<T> type() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CommandContextKey<?> that)) {
            return false;
        }
        return name.equals(that.name) && type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }

    @Override
    public String toString() {
        return "CommandContextKey[name=%s, type=%s]".formatted(name, type.getName());
    }

    private static String normalizeName(String value) {
        Objects.requireNonNull(value, "name");
        String normalized = value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("CommandContextKey name must not be blank");
        }
        return normalized;
    }
}

