package irden.space.proxy.plugin.planet_protect.permissions;

import irden.space.proxy.plugin.api.PermissionEnum;
import irden.space.proxy.plugin.api.annotations.RegisterPluginPermissions;


@RegisterPluginPermissions
public enum PlanetPermissions implements PermissionEnum {
    MODIFY_LIMIT("planet.protect.modify");

    private final String permissionNode;

    PlanetPermissions(String permissionNode) {
        this.permissionNode = permissionNode;
    }

    @Override
    public String permissionNode() {
        return permissionNode;
    }
}

