package irden.space.proxy.plugin.player_manager;

import irden.space.proxy.plugin.api.Permission;
import irden.space.proxy.plugin.api.PermissionRegistry;
import lombok.experimental.UtilityClass;

import irden.space.proxy.plugin.api.Permission;
import irden.space.proxy.plugin.api.PermissionRegistry;

public enum ChatPermissions {
    SENT("starry.chat.sent"),
    RECIEVE("starry.chat.recieve");

    private final String permissionNode;
    private Permission permission;

    ChatPermissions(String permissionNode) {
        this.permissionNode = permissionNode;
    }

    public Permission permission() {
        if (permission == null) {
            throw new IllegalStateException("Chat permissions not registered yet. Call registerDefaults() first.");
        }
        return permission;
    }

    public static synchronized void registerDefaults() {
        for (ChatPermissions perm : values()) {
            if (perm.permission == null) {
                perm.permission = PermissionRegistry.registerIfAbsent(perm.permissionNode);
            }
        }
    }
}