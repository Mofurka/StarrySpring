package irden.space.proxy.plugin.site.permissions;

import irden.space.proxy.plugin.api.PermissionEnum;
import irden.space.proxy.plugin.api.annotations.RegisterPluginPermissions;

@RegisterPluginPermissions
public enum LinkedPermissions implements PermissionEnum {
    USER_LINKED("site.user.linked");

    private final String permissionNode;

    LinkedPermissions(String permissionNode) {
        this.permissionNode = permissionNode;
    }

    @Override
    public String permissionNode() {
        return permissionNode;
    }
}