package irden.space.proxy.plugin.chat_manager;

import irden.space.proxy.plugin.api.PermissionEnum;
import irden.space.proxy.plugin.api.annotations.RegisterPluginPermissions;

@RegisterPluginPermissions
public enum ChatPermissions implements PermissionEnum {
    INVISIBLE_BYPASS("chat.invisible_bypass"),
    JOIN_ANNOUNCE("chat.join_announce"),
    UNIVERSE_CHAT("chat.universe");

    private final String permissionNode;

    ChatPermissions(String permissionNode) {
        this.permissionNode = permissionNode;
    }

    @Override
    public String permissionNode() {
        return permissionNode;
    }
}
