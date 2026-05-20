package irden.space.proxy.plugin.player_manager.model;

import irden.space.proxy.plugin.api.PermissionSet;
import irden.space.proxy.plugin.api.PermissionView;

import java.util.Collection;

public final class UserPermissions implements PermissionView {

    private final PermissionSet grantedPermissions;
    private final PermissionSet revokedPermissions;

    public UserPermissions(Collection<StarryRole> starryRoles, PermissionSet extraPermissions) {
        this(starryRoles, extraPermissions, null);
    }

    public UserPermissions(Collection<StarryRole> starryRoles, PermissionSet extraPermissions, PermissionSet revokedPermissions) {

        PermissionSet result = new PermissionSet();

        if (starryRoles != null) {
            for (StarryRole starryRole : starryRoles) {
                if (starryRole != null) {
                    result.merge(starryRole.effectivePermissions());
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
