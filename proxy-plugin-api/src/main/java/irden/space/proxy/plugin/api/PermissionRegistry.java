package irden.space.proxy.plugin.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PermissionRegistry {

    private static final Map<String, Integer> permissions = new LinkedHashMap<>();
    public static final Permission ALL = registerIfAbsent("all");

    private PermissionRegistry() {
    }

    public static synchronized Permission registerIfAbsent(String name) {
        validatePermissionName(name);

        Integer existingId = permissions.get(name);
        if (existingId != null) {
            return new Permission(name, existingId);
        }

        int id = permissions.size();
        permissions.put(name, id);
        return new Permission(name, id);
    }

    public static synchronized List<Permission> registerAll(String... names) {
        if (names == null || names.length == 0) {
            return List.of();
        }

        List<Permission> registeredPermissions = new ArrayList<>(names.length);
        for (String name : names) {
            registeredPermissions.add(registerIfAbsent(name));
        }

        return List.copyOf(registeredPermissions);
    }

    public static synchronized int registerPermission(String name) {
        validatePermissionName(name);

        if (permissions.containsKey(name)) {
            throw new IllegalArgumentException("Permission already exists: " + name);
        }

        int id = permissions.size();
        permissions.put(name, id);
        return id;
    }

    public static synchronized Permission getPermission(String name) {
        return new Permission(name, getPermissionId(name));
    }

    public static synchronized int getPermissionId(String name) {
        validatePermissionName(name);

        Integer id = permissions.get(name);
        if (id == null) {
            throw new IllegalArgumentException("Unknown permission: " + name);
        }
        return id;
    }

    public static synchronized int permissionCount() {
        return permissions.size();
    }

    public static synchronized Map<String, Integer> entries() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(permissions));
    }

    public static synchronized List<Integer> resolveWildcard(String pattern) {
        validateWildcardPattern(pattern);

        String prefix = pattern.substring(0, pattern.length() - 1);
        List<Integer> resolvedPermissions = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : permissions.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                resolvedPermissions.add(entry.getValue());
            }
        }

        return List.copyOf(resolvedPermissions);
    }

    private static void validatePermissionName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Permission name must not be blank");
        }
    }

    private static void validateWildcardPattern(String pattern) {
        validatePermissionName(pattern);

        if (!pattern.endsWith("*")) {
            throw new IllegalArgumentException("Wildcard permission must end with '*': " + pattern);
        }
        if (pattern.indexOf('*') != pattern.length() - 1) {
            throw new IllegalArgumentException("Wildcard permission may contain '*' only as the last character: " + pattern);
        }
    }
}
