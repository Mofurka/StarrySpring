package irden.space.proxy.plugin.api;

import java.util.Collection;

@FunctionalInterface
public interface PermissionView {

    PermissionView EMPTY = permissionId -> false;

    boolean has(int permissionId);

    default boolean has(Permission permission) {
        if (permission == null) {
            return false;
        }

        return has(permission.id());
    }

    default boolean hasAny(Permission... permissions) {
        if (permissions == null) {
            return false;
        }

        for (Permission permission : permissions) {
            if (has(permission)) {
                return true;
            }
        }

        return false;
    }

    default boolean hasAny(Collection<Permission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }

        for (Permission permission : permissions) {
            if (has(permission)) {
                return true;
            }
        }

        return false;
    }

    default boolean hasAll(Permission... permissions) {
        if (permissions == null) {
            return false;
        }

        for (Permission permission : permissions) {
            if (!has(permission)) {
                return false;
            }
        }

        return true;
    }

    default boolean hasAll(Collection<Permission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }

        for (Permission permission : permissions) {
            if (!has(permission)) {
                return false;
            }
        }

        return true;
    }
}
