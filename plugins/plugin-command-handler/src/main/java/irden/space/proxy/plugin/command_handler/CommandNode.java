package irden.space.proxy.plugin.command_handler;

import irden.space.proxy.plugin.api.Permission;

import java.util.List;

public interface CommandNode {

    String name();

    String description();

    List<CommandNode> children();

    CommandExecutor executor();

    List<Permission> requiredPermissions();

    default boolean hasExecutor() {
        return executor() != null;
    }

    default boolean hasRequiredPermissions() {
        return !requiredPermissions().isEmpty();
    }
}