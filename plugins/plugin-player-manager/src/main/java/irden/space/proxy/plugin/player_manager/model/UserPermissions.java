package irden.space.proxy.plugin.player_manager.model;

import irden.space.proxy.plugin.api.PermissionSet;
import irden.space.proxy.plugin.api.PermissionView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class UserPermissions implements PermissionView {

    private final PermissionSet grantedPermissions;
    private final PermissionSet revokedPermissions;

    public UserPermissions(Collection<Role> roles, PermissionSet extraPermissions) {
        this(roles, extraPermissions, null);
    }

    public UserPermissions(Collection<Role> roles, PermissionSet extraPermissions, PermissionSet revokedPermissions) {

        PermissionSet result = new PermissionSet();

        if (roles != null) {
            for (Role role : roles) {
                if (role != null) {
                    result.merge(role.effectivePermissions());
                }
            }
        }

        if (extraPermissions != null) {
            result.merge(extraPermissions);
        }

        this.grantedPermissions = result;
        this.revokedPermissions = revokedPermissions == null ? new PermissionSet() : revokedPermissions.copy();
    }

    public PermissionSet effectivePermissions() {
        return grantedPermissions.copy();
    }

    public PermissionSet revokedPermissions() {
        return revokedPermissions.copy();
    }

    @Override
    public boolean has(int permissionId) {
        return !revokedPermissions.has(permissionId) && grantedPermissions.has(permissionId);
    }
}
