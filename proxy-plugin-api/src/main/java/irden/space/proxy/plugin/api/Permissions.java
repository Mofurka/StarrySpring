package irden.space.proxy.plugin.api;

import java.util.Collection;

public final class Permissions {

    private Permissions() {
    }

    public static PermissionView view(PluginSessionContext context) {
        return context == null ? PermissionView.EMPTY : context.permissions();
    }

    public static PermissionSet none() {
        return new PermissionSet();
    }

    public static PermissionSet granted(Permission... permissions) {
        PermissionSet permissionSet = new PermissionSet();
        permissionSet.grantAll(permissions);
        return permissionSet;
    }

    public static PermissionSet granted(Collection<Permission> permissions) {
        PermissionSet permissionSet = new PermissionSet();
        permissionSet.grantAll(permissions);
        return permissionSet;
    }

    public static PermissionSet allAccess() {
        PermissionSet permissionSet = new PermissionSet();
        permissionSet.grantAllAccess();
        return permissionSet;
    }

    public static boolean has(PluginSessionContext context, Permission permission) {
        return view(context).has(permission);
    }

    public static boolean hasAny(PluginSessionContext context, Permission... permissions) {
        return view(context).hasAny(permissions);
    }

    public static boolean hasAll(PluginSessionContext context, Permission... permissions) {
        return view(context).hasAll(permissions);
    }
}

