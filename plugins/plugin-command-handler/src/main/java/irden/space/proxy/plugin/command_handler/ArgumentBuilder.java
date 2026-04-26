package irden.space.proxy.plugin.command_handler;

import irden.space.proxy.plugin.api.Permission;

import java.util.ArrayList;
import java.util.List;

public final class ArgumentBuilder<T> implements CommandNodeBuilder<ArgumentNode<T>> {

    private final String name;
    private final ArgumentType<T> type;
    private String description = "";
    private boolean required = true;
    private final List<CommandNode> children = new ArrayList<>();
    private final List<Permission> requiredPermissions = new ArrayList<>();
    private CommandExecutor executor;

    ArgumentBuilder(String name, ArgumentType<T> type) {
        this.name = name;
        this.type = type;
    }

    public ArgumentBuilder<T> description(String description) {
        this.description = description;
        return this;
    }

    public ArgumentBuilder<T> optional() {
        this.required = false;
        return this;
    }

    public ArgumentBuilder<T> required() {
        this.required = true;
        return this;
    }

    public ArgumentBuilder<T> then(CommandNode node) {
        children.add(node);
        return this;
    }

    public ArgumentBuilder<T> then(CommandNodeBuilder<?> builder) {
        children.add(builder.buildNode());
        return this;
    }

    public ArgumentBuilder<T> permission(Permission permission) {
        if (permission == null) {
            throw new IllegalArgumentException("Permission must not be null");
        }

        requiredPermissions.add(permission);
        return this;
    }

    public ArgumentBuilder<T> permissions(Permission... permissions) {
        if (permissions == null) {
            return this;
        }

        for (Permission permission : permissions) {
            permission(permission);
        }

        return this;
    }

    public ArgumentBuilder<T> executes(CommandExecutor executor) {
        this.executor = executor;
        return this;
    }

    @Override
    public ArgumentNode<T> buildNode() {
        return new ArgumentNode<>(name, description, type, required, children, executor, requiredPermissions);
    }
}