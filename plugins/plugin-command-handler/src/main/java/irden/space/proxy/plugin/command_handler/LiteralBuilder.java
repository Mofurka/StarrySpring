package irden.space.proxy.plugin.command_handler;

import irden.space.proxy.plugin.api.Permission;

import java.util.ArrayList;
import java.util.List;

public final class LiteralBuilder implements CommandNodeBuilder<LiteralNode> {

    private final String name;
    private String description = "";
    private final List<CommandNode> children = new ArrayList<>();
    private final List<Permission> requiredPermissions = new ArrayList<>();
    private CommandExecutor executor;

    LiteralBuilder(String name) {
        this.name = name;
    }

    public LiteralBuilder description(String description) {
        this.description = description;
        return this;
    }

    public LiteralBuilder then(CommandNode node) {
        children.add(node);
        return this;
    }

    public LiteralBuilder then(CommandNodeBuilder<?> builder) {
        children.add(builder.buildNode());
        return this;
    }

    public LiteralBuilder permission(Permission permission) {
        if (permission == null) {
            throw new IllegalArgumentException("Permission must not be null");
        }

        requiredPermissions.add(permission);
        return this;
    }

    public LiteralBuilder permissions(Permission... permissions) {
        if (permissions == null) {
            return this;
        }

        for (Permission permission : permissions) {
            permission(permission);
        }

        return this;
    }

    public LiteralBuilder executes(CommandExecutor executor) {
        this.executor = executor;
        return this;
    }

    public CommandSpec build() {
        return new CommandSpec(buildNode());
    }

    @Override
    public LiteralNode buildNode() {
        return new LiteralNode(name, description, children, executor, requiredPermissions);
    }
}