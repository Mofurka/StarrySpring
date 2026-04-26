package irden.space.proxy.plugin.api;

import java.util.BitSet;
import java.util.Collection;
import java.util.List;

public class PermissionSet implements PermissionView {

    private final BitSet permissions = new BitSet();

    public void grant(int permissionId) {
        if (permissionId < 0) {
            throw new IllegalArgumentException("Permission id must not be negative: " + permissionId);
        }

        permissions.set(permissionId);
    }

    public void grant(Permission permission) {
        if (permission == null) {
            throw new IllegalArgumentException("Permission must not be null");
        }

        grant(permission.id());
    }

    public void grantAll(Permission... permissions) {
        if (permissions == null) {
            return;
        }

        for (Permission permission : permissions) {
            if (permission != null) {
                grant(permission);
            }
        }
    }

    public void grantAll(Collection<Permission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return;
        }

        for (Permission permission : permissions) {
            if (permission != null) {
                grant(permission);
            }
        }
    }

    public void grantAllAccess() {
        grant(PermissionRegistry.ALL);
    }

    public void revoke(int permissionId) {
        if (permissionId < 0) {
            return;
        }

        permissions.clear(permissionId);
    }

    public void revoke(Permission permission) {
        if (permission == null) {
            return;
        }

        revoke(permission.id());
    }

    public void revokeAll(Permission... permissions) {
        if (permissions == null) {
            return;
        }

        for (Permission permission : permissions) {
            if (permission != null) {
                revoke(permission);
            }
        }
    }

    public void revokeAll(Collection<Permission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return;
        }

        for (Permission permission : permissions) {
            if (permission != null) {
                revoke(permission);
            }
        }
    }

    public void grantAllIds(Collection<Integer> permissionIds) {
        if (permissionIds == null) {
            return;
        }

        for (Integer permissionId : permissionIds) {
            if (permissionId != null) {
                grant(permissionId);
            }
        }
    }

    public void merge(PermissionSet other) {
        if (other == null) {
            return;
        }

        this.permissions.or(other.permissions);
    }

    public boolean grantsAll() {
        return permissions.get(PermissionRegistry.ALL.id());
    }

    @Override
    public boolean has(int permissionId) {
        if (permissionId < 0) {
            return false;
        }

        int allPermissionId = PermissionRegistry.ALL.id();
        if (permissionId == allPermissionId) {
            return permissions.get(allPermissionId);
        }

        return permissions.get(allPermissionId) || permissions.get(permissionId);
    }

    public boolean isEmpty() {
        return permissions.isEmpty();
    }

    public PermissionSet copy() {
        PermissionSet copy = new PermissionSet();
        copy.permissions.or(this.permissions);
        return copy;
    }
}
