package irden.space.proxy.plugin.irden.integration.permissions;

import irden.space.proxy.plugin.api.PermissionEnum;
import irden.space.proxy.plugin.api.annotations.RegisterPluginPermissions;

@RegisterPluginPermissions
public enum SitePermissions implements PermissionEnum {
    USER_LINKED("site.user.linked"),
    IRDEN_STORAGE_AUTO_ACCEPT("site.storage.auto_accept");

    private final String permissionNode;

    SitePermissions(String permissionNode) {
        this.permissionNode = permissionNode;
    }

    @Override
    public String permissionNode() {
        return permissionNode;
    }
}