package irden.space.proxy.plugin.command_handler;

import irden.space.proxy.plugin.api.Permission;

import java.util.List;
import java.util.Objects;

public record LiteralNode(String name, String description, List<CommandNode> children, CommandExecutor executor,
                          List<Permission> requiredPermissions) implements CommandNode {

    public LiteralNode(
            String name,
            String description,
            List<CommandNode> children,
            CommandExecutor executor,
            List<Permission> requiredPermissions
    ) {
        this.name = normalizeName(name);
        this.description = description == null ? "" : description.trim();
        this.children = List.copyOf(children);
        this.executor = executor;
        this.requiredPermissions = List.copyOf(requiredPermissions);
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