package irden.space.proxy.plugin.runtime_admin;

import irden.space.proxy.plugin.api.PermissionEnum;

public enum PluginRuntimeAdminPermissions implements PermissionEnum {
    LIST("plugin.runtime.list"),
    START("plugin.runtime.start"),
    STOP("plugin.runtime.stop"),
    RELOAD("plugin.runtime.reload");

    private final String permissionNode;

    PluginRuntimeAdminPermissions(String permissionNode) {
        this.permissionNode = permissionNode;
    }

    @Override
    public String permissionNode() {
        return permissionNode;
    }
}
