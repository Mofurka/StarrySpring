package irden.space.proxy.plugin.command_handler;

import java.util.List;
import java.util.Objects;

public final class ArgumentCommandNode<T> implements CommandNode {

    private final String name;
    private final String description;
    private final ArgumentType<T> type;
    private final boolean optional;
    private final List<CommandNode> children;
    private final CommandExecutor executor;

    public ArgumentCommandNode(
            String name,
            String description,
            ArgumentType<T> type,
            boolean optional,
            List<CommandNode> children,
            CommandExecutor executor
    ) {
        this.name = normalizeName(name);
        this.description = description == null ? "" : description.trim();
        this.type = Objects.requireNonNull(type, "type");
        this.optional = optional;
        this.children = List.copyOf(Objects.requireNonNull(children, "children"));
        this.executor = executor;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return description;
    }

    public ArgumentType<T> type() {
        return type;
    }

    public boolean optional() {
        return optional;
    }

    @Override
    public List<CommandNode> children() {
        return children;
    }

    @Override
    public CommandExecutor executor() {
        return executor;
    }

    private static String normalizeName(String name) {
        Objects.requireNonNull(name, "name");
        String normalized = name.trim();

        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Argument name must not be blank");
        }

        if (normalized.contains(" ")) {
            throw new IllegalArgumentException("Argument name must not contain spaces: " + name);
        }

        return normalized;
    }
}