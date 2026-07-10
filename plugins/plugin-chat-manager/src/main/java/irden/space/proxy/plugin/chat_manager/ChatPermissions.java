package irden.space.proxy.plugin.chat_manager;

import irden.space.proxy.plugin.api.PermissionEnum;
import irden.space.proxy.plugin.api.annotations.RegisterPluginPermissions;

@RegisterPluginPermissions
public enum ChatPermissions implements PermissionEnum {
    INVISIBLE_JOIN("chat.invisible_join"),
    INVISIBLE_BYPASS("chat.invisible_bypass"),;

    private final String permissionNode;

    ChatPermissions(String permissionNode) {
        this.permissionNode = permissionNode;
    }

    @Override
    public String permissionNode() {
        return permissionNode;
    }
}
