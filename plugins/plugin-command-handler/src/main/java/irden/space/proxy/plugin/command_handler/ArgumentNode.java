package irden.space.proxy.plugin.command_handler;

import irden.space.proxy.plugin.api.Permission;

import java.util.List;
import java.util.Objects;

public record ArgumentNode<T>(String name, String description, ArgumentType<T> type, boolean required,
                              List<CommandNode> children, CommandExecutor executor,
                              List<Permission> requiredPermissions) implements CommandNode {

    public ArgumentNode(
            String name,
            String description,
            ArgumentType<T> type,
            boolean required,
            List<CommandNode> children,
            CommandExecutor executor,
            List<Permission> requiredPermissions
    ) {
        this.name = normalizeName(name);
        this.description = description == null ? "" : description.trim();
        this.type = Objects.requireNonNull(type, "type");
        this.required = required;
        this.children = List.copyOf(children);
        this.executor = executor;
        this.requiredPermissions = List.copyOf(requiredPermissions);

        if (type.greedy() && !children.isEmpty()) {
            throw new IllegalArgumentException("Greedy argument cannot have children: " + name);
        }
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