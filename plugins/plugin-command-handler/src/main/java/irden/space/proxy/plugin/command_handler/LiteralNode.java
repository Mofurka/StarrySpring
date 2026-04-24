package irden.space.proxy.plugin.command_handler;

import java.util.List;
import java.util.Objects;

public final class LiteralNode implements CommandNode {

    private final String name;
    private final String description;
    private final List<CommandNode> children;
    private final CommandExecutor executor;

    public LiteralNode(
            String name,
            String description,
            List<CommandNode> children,
            CommandExecutor executor
    ) {
        this.name = normalizeName(name);
        this.description = description == null ? "" : description.trim();
        this.children = List.copyOf(children);
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
        String normalized = value.trim().toLowerCase();

        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Literal name must not be blank");
        }

        if (normalized.contains(" ")) {
            throw new IllegalArgumentException("Literal name must not contain spaces: " + value);
        }

        return normalized;
    }
}