package irden.space.proxy.plugin.player_manager.model;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

public final class Role {

    private final String name;
    private final irden.space.proxy.plugin.api.PermissionSet permissions = new irden.space.proxy.plugin.api.PermissionSet();
    private final List<Role> parents = new ArrayList<>();

    public Role(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public irden.space.proxy.plugin.api.PermissionSet effectivePermissions() {
        return effectivePermissions(java.util.Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private irden.space.proxy.plugin.api.PermissionSet effectivePermissions(Set<Role> visitedRoles) {
        if (!visitedRoles.add(this)) {
            throw new IllegalStateException("Cyclic role inheritance detected for role: " + name);
        }

        irden.space.proxy.plugin.api.PermissionSet result = permissions.copy();

        for (Role parent : parents) {
            result.merge(parent.effectivePermissions(visitedRoles));
        }

        visitedRoles.remove(this);

        return result;
    }

    public void inherit(Role parent) {
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

    public irden.space.proxy.plugin.api.PermissionSet permissions() {
        return permissions;
    }
}
