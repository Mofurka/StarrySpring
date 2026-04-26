package irden.space.proxy.plugin.command_handler;

import irden.space.proxy.plugin.api.Permission;

import java.util.List;
import java.util.Objects;

public record ArgumentCommandNode<T>(String name, String description, ArgumentType<T> type, boolean optional,
                                     List<CommandNode> children, CommandExecutor executor) implements CommandNode {

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
    public List<Permission> requiredPermissions() {
        return List.of();
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