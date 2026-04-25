package irden.space.proxy.plugin.player_manager.model;

import irden.space.proxy.plugin.api.PermissionSet;
import irden.space.proxy.plugin.api.PermissionView;

import java.util.Collection;

public final class UserPermissions implements PermissionView {

    private final PermissionSet effectivePermissions;

    public UserPermissions(Collection<Role> roles, PermissionSet extraPermissions) {

        PermissionSet result = new PermissionSet();

        if (roles != null) {
            for (Role role : roles) {
                if (role != null) {
                    result.merge(role.effectivePermissions());
                }
            }
        }

        result.merge(extraPermissions);

        this.effectivePermissions = result;
    }

    public PermissionSet effectivePermissions() {
        return effectivePermissions.copy();
    }

    @Override
    public boolean has(int permissionId) {
        return effectivePermissions.has(permissionId);
    }
}
