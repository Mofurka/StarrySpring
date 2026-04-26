package irden.space.proxy.plugin.player_manager.roles;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import irden.space.proxy.plugin.api.PermissionRegistry;
import irden.space.proxy.plugin.player_manager.model.Role;
import irden.space.proxy.plugin.player_manager.permissions.PermissionResolver;
import irden.space.proxy.plugin.player_manager.permissions.model.StarryRoles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class RoleManager {
    public static final String OWNER_ROLE_NAME = "Owner";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(JsonParser.Feature.ALLOW_COMMENTS, true);

    private final Path configPath;
    private final PermissionResolver permissionResolver = new PermissionResolver();
    private StarryRoles rolesConfig;
    private Map<String, Role> rolesByName = Map.of();

    public RoleManager() {
        this(Path.of("config/permissions.jsonc"));
    }

    public RoleManager(Path configPath) {
        this.configPath = Objects.requireNonNull(configPath, "configPath");
        reload();
    }

    public synchronized void reload() {
        this.rolesConfig = loadOrCreateConfig();
        this.rolesByName = buildRoles(this.rolesConfig);
    }

    public synchronized Map<String, Role> rolesByName() {
        return Map.copyOf(rolesByName);
    }

    public synchronized Optional<Role> findRole(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return Optional.empty();
        }

        return Optional.ofNullable(rolesByName.get(roleName));
    }

    public synchronized boolean isOwner(String playerUuid) {
        String configuredOwnerUuid = rolesConfig.getOwnerUuid();
        return configuredOwnerUuid != null && configuredOwnerUuid.equalsIgnoreCase(playerUuid);
    }

    public synchronized String ownerUuid() {
        return rolesConfig.getOwnerUuid();
    }

    public synchronized String defaultRoleName() {
        return rolesConfig.getDefaultAccount();
    }

    public synchronized List<String> resolveRoleNamesForPlayer(
            String playerUuid,
            String accountName,
            Collection<String> assignedRoleNames
    ) {
        LinkedHashSet<String> resolvedRoleNames = new LinkedHashSet<>();

        if (isOwner(playerUuid)) {
            resolvedRoleNames.add(OWNER_ROLE_NAME);
            return List.copyOf(resolvedRoleNames);
        }

        String preferredRoleName = accountName;
        if (preferredRoleName == null || preferredRoleName.isBlank() || !rolesByName.containsKey(preferredRoleName)) {
            preferredRoleName = rolesConfig.getDefaultAccount();
        }
        if (preferredRoleName != null && !preferredRoleName.isBlank() && !OWNER_ROLE_NAME.equalsIgnoreCase(preferredRoleName)) {
            resolvedRoleNames.add(preferredRoleName);
        }

        if (assignedRoleNames != null) {
            for (String assignedRoleName : assignedRoleNames) {
                if (assignedRoleName != null
                        && !assignedRoleName.isBlank()
                        && !OWNER_ROLE_NAME.equalsIgnoreCase(assignedRoleName)
                        && rolesByName.containsKey(assignedRoleName)) {
                    resolvedRoleNames.add(assignedRoleName);
                }
            }
        }

        return List.copyOf(resolvedRoleNames);
    }

    private StarryRoles loadOrCreateConfig() {
        try {
            Path parent = configPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            if (Files.exists(configPath)) {
                return OBJECT_MAPPER.readValue(configPath.toFile(), StarryRoles.class);
            }

            StarryRoles defaultRoles = new StarryRoles();
            OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(configPath.toFile(), defaultRoles);
            return defaultRoles;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load permissions configuration: " + configPath, ex);
        }
    }

    private Map<String, Role> buildRoles(StarryRoles config) {
        Map<String, Role> resolvedRoles = new LinkedHashMap<>();

        Role ownerRole = new Role(OWNER_ROLE_NAME);
        ownerRole.permissions().grantAllAccess();
        resolvedRoles.put(OWNER_ROLE_NAME, ownerRole);

        List<StarryRoles.StarryRole> configuredRoles = config.getAccounts() == null ? List.of() : config.getAccounts();
        for (StarryRoles.StarryRole configuredRole : configuredRoles) {
            String roleName = normalizeRoleName(configuredRole.getName());
            if (resolvedRoles.containsKey(roleName)) {
                throw new IllegalStateException("Duplicate role name in permissions configuration: " + roleName);
            }
            resolvedRoles.put(roleName, new Role(roleName));
        }

        for (StarryRoles.StarryRole configuredRole : configuredRoles) {
            for (String permissionRule : safeList(configuredRole.getPermissions())) {
                if (permissionRule != null && !permissionRule.isBlank() && !permissionRule.endsWith("*")) {
                    PermissionRegistry.registerIfAbsent(permissionRule);
                }
            }
        }

        for (StarryRoles.StarryRole configuredRole : configuredRoles) {
            Role role = requireRole(resolvedRoles, configuredRole.getName());
            role.permissions().merge(permissionResolver.resolveRules(safeList(configuredRole.getPermissions())));
        }

        for (StarryRoles.StarryRole configuredRole : configuredRoles) {
            Role role = requireRole(resolvedRoles, configuredRole.getName());
            for (String inheritedRoleName : safeList(configuredRole.getInherits())) {
                role.inherit(requireRole(resolvedRoles, inheritedRoleName));
            }
        }

        String defaultRoleName = normalizeRoleName(config.getDefaultAccount());
        if (OWNER_ROLE_NAME.equalsIgnoreCase(defaultRoleName)) {
            throw new IllegalStateException("Default account role cannot be '%s' because Owner is reserved for ownerUuid".formatted(OWNER_ROLE_NAME));
        }
        if (!resolvedRoles.containsKey(defaultRoleName)) {
            throw new IllegalStateException("Default account role is not defined in permissions configuration: " + defaultRoleName);
        }

        return Map.copyOf(resolvedRoles);
    }

    private Role requireRole(Map<String, Role> roles, String roleName) {
        String normalizedRoleName = normalizeRoleName(roleName);
        Role role = roles.get(normalizedRoleName);
        if (role == null) {
            throw new IllegalStateException("Unknown role in permissions configuration: " + normalizedRoleName);
        }
        return role;
    }

    private static List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private static String normalizeRoleName(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            throw new IllegalStateException("Role name must not be blank in permissions configuration");
        }
        return roleName.trim();
    }

}
