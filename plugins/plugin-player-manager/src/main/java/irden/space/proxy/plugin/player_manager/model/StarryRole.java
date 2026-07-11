package irden.space.proxy.plugin.player_manager.model;

import irden.space.proxy.plugin.api.PermissionSet;

import java.util.*;

public final class StarryRole {

    private final String name;
    private final String colorPrefix;
    private final PermissionSet permissions = new PermissionSet();
    private final PermissionSet revokedPermissions = new PermissionSet();
    private final List<StarryRole> parents = new ArrayList<>();

    public StarryRole(String name, String colorPrefix) {
        this.name = name;
        this.colorPrefix = colorPrefix;
    }

    public String name() {
        return name;
    }

    public String colorPrefix() {
        return colorPrefix;
    }

    public PermissionSet effectivePermissions() {
        return effectivePermissions(Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private PermissionSet effectivePermissions(Set<StarryRole> visitedStarryRoles) {
        if (!visitedStarryRoles.add(this)) {
            throw new IllegalStateException("Cyclic role inheritance detected for role: " + name);
        }

        PermissionSet result = permissions.copy();

        for (StarryRole parent : parents) {
            result.merge(parent.effectivePermissions(visitedStarryRoles));
        }

        visitedStarryRoles.remove(this);

        return result;
    }

    public PermissionSet effectiveRevokedPermissions() {
        return effectiveRevokedPermissions(Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private PermissionSet effectiveRevokedPermissions(Set<StarryRole> visitedStarryRoles) {
        if (!visitedStarryRoles.add(this)) {
            throw new IllegalStateException("Cyclic role inheritance detected for role: " + name);
        }

        PermissionSet result = revokedPermissions.copy();

        for (StarryRole parent : parents) {
            result.merge(parent.effectiveRevokedPermissions(visitedStarryRoles));
        }

        visitedStarryRoles.remove(this);

        return result;
    }

    public void inherit(StarryRole parent) {
        if (parent == null) {
            throw new IllegalArgumentException("Parent role must not be null");
        }
        if (parent == this) {
            throw new IllegalArgumentException("Role cannot inherit itself: " + name);
        }
        if (parents.contains(parent)) {
            return;
        }

        parents.add(parent);
    }

    public PermissionSet permissions() {
        return permissions;
    }

    public PermissionSet revokedPermissions() {
        return revokedPermissions;
    }
}
