package irden.space.proxy.plugin.command_handler;

import java.util.List;
import java.util.Objects;

public final class ArgumentNode<T> implements CommandNode {

    private final String name;
    private final String description;
    private final ArgumentType<T> type;
    private final boolean required;
    private final List<CommandNode> children;
    private final CommandExecutor executor;

    public ArgumentNode(
            String name,
            String description,
            ArgumentType<T> type,
            boolean required,
            List<CommandNode> children,
            CommandExecutor executor
    ) {
        this.name = normalizeName(name);
        this.description = description == null ? "" : description.trim();
        this.type = Objects.requireNonNull(type, "type");
        this.required = required;
        this.children = List.copyOf(children);
        this.executor = executor;

        if (type.greedy() && !children.isEmpty()) {
            throw new IllegalArgumentException("Greedy argument cannot have children: " + name);
        }
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

    public boolean required() {
        return required;
    }

    @Override
    public List<CommandNode> children() {
        return children;
    }

    @Override
    public CommandExecutor executor() {
        return executor;
    }

    private String normalizeName(String value) {
        Objects.requireNonNull(value, "name");
        String normalized = value.trim();

        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Argument name must not be blank");
        }

        if (normalized.contains(" ")) {
            throw new IllegalArgumentException("Argument name must not contain spaces: " + value);
        }

        return normalized;
    }
}