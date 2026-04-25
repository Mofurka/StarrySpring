package irden.space.proxy.plugin.player_manager.permissions;

import irden.space.proxy.plugin.api.PermissionRegistry;
import irden.space.proxy.plugin.api.PermissionSet;

import java.util.Collection;

public final class PermissionResolver {

    public PermissionSet resolveRules(Collection<String> rules) {
        PermissionSet resolvedPermissions = new PermissionSet();

        if (rules == null) {
            return resolvedPermissions;
        }

        for (String rule : rules) {
            resolvedPermissions.merge(resolveRule(rule));
        }

        return resolvedPermissions;
    }

    public PermissionSet resolveRule(String rule) {
        if (rule == null || rule.isBlank()) {
            throw new IllegalArgumentException("Permission rule must not be blank");
        }

        PermissionSet resolvedPermissions = new PermissionSet();
        if (rule.endsWith("*")) {
            resolvedPermissions.grantAllIds(PermissionRegistry.resolveWildcard(rule));
            return resolvedPermissions;
        }

        resolvedPermissions.grant(PermissionRegistry.getPermissionId(rule));
        return resolvedPermissions;
    }
}

