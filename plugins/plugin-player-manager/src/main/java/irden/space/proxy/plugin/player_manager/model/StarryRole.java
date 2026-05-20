package irden.space.proxy.plugin.player_manager.model;

import irden.space.proxy.plugin.api.PermissionSet;

import java.util.*;

public final class StarryRole {

    private final String name;
    private final PermissionSet permissions = new PermissionSet();
    private final List<StarryRole> parents = new ArrayList<>();

    public StarryRole(String name) {
        this.name = name;
    }

    public String name() {
        return name;
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
}
