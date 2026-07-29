package irden.space.proxy.plugin.command_handler;

import irden.space.proxy.plugin.api.Permission;

import java.util.List;
import java.util.Set;

public interface CommandNode {

    String name();

    String description();

    List<CommandNode> children();

    CommandExecutor executor();

    List<Permission> requiredPermissions();

    default Set<CommandSurface> surfaces() {
        return Set.of();
    }

    default boolean hasExecutor() {
        return executor() != null;
    }

    default boolean hasRequiredPermissions() {
        return !requiredPermissions().isEmpty();
    }

    default boolean isExportedTo(CommandSurface surface) {
        Set<CommandSurface> surfaces = surfaces();
        return surfaces.isEmpty() || surfaces.contains(surface);
    }
}