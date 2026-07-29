package irden.space.proxy.plugin.irden.permissions;

import irden.space.proxy.plugin.api.PermissionEnum;
import irden.space.proxy.plugin.api.annotations.RegisterPluginPermissions;

@RegisterPluginPermissions
public enum BalancePermissions implements PermissionEnum {
    BALANCE_MANAGEMENT("balance.management");

    private final String permissionNode;

    BalancePermissions(String permissionNode) {
        this.permissionNode = permissionNode;
    }

    @Override
    public String permissionNode() {
        return permissionNode;
    }
}
