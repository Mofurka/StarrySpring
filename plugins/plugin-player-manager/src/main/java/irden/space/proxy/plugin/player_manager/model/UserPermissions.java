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

        PermissionSet grantedResult = new PermissionSet();
        PermissionSet revokedResult = new PermissionSet();

        if (starryRoles != null) {
            for (StarryRole starryRole : starryRoles) {
                if (starryRole != null) {
                    grantedResult.merge(starryRole.effectivePermissions());
                    revokedResult.merge(starryRole.effectiveRevokedPermissions());
                }
            }
        }

        if (extraPermissions != null) {
            grantedResult.merge(extraPermissions);
        }

        if (revokedPermissions != null) {
            revokedResult.merge(revokedPermissions);
        }

        this.grantedPermissions = grantedResult;
        this.revokedPermissions = revokedResult;
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
