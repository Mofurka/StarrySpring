package irden.space.proxy.plugin.player_manager;

import irden.space.proxy.plugin.api.PermissionEnum;

public enum PlayerManagerPermissions implements PermissionEnum {
    USER("player.user");

    private final String permissionNode;

    PlayerManagerPermissions(String permissionNode) {
        this.permissionNode = permissionNode;
    }

    @Override
    public String permissionNode() {
        return permissionNode;
    }
}