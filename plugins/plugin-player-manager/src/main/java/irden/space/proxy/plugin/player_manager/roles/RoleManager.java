package irden.space.proxy.plugin.player_manager.roles;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import irden.space.proxy.plugin.api.PermissionRegistry;
import irden.space.proxy.plugin.player_manager.model.StarryRole;
import irden.space.proxy.plugin.player_manager.permissions.PermissionResolver;
import irden.space.proxy.plugin.player_manager.permissions.model.StarryRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Component
public final class RoleManager {
    public static final String OWNER_ROLE_NAME = "Owner";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(JsonParser.Feature.ALLOW_COMMENTS, true);

    private static final String DEFAULT_CONFIG_RESOURCE = "config/permissions.jsonc";

    private final Path configPath;
    private final PermissionResolver permissionResolver;
    private StarryRoles rolesConfig;
    private Map<String, StarryRole> rolesByName = Map.of();

    @Autowired
    public RoleManager(
            @Value("${starry.player-manager.permissions-path:config/plugins/player-manager/permissions.jsonc}") String configPath,
            PermissionResolver permissionResolver
    ) {
        this(Path.of(configPath), permissionResolver);
    }

    public RoleManager(Path configPath, PermissionResolver permissionResolver) {
        this.configPath = Objects.requireNonNull(configPath, "configPath");
        this.permissionResolver = Objects.requireNonNull(permissionResolver, "permissionResolver");
        reload();
    }

    public synchronized void reload() {
        this.rolesConfig = loadOrCreateConfig();
        this.rolesByName = buildRoles(this.rolesConfig);
    }

    public synchronized Map<String, StarryRole> rolesByName() {
        return Map.copyOf(rolesByName);
    }

    public synchronized Optional<StarryRole> findRole(String roleName) {
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

            if (copyBundledDefault(configPath)) {
                return OBJECT_MAPPER.readValue(configPath.toFile(), StarryRoles.class);
            }

            StarryRoles defaultRoles = new StarryRoles();
            OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(configPath.toFile(), defaultRoles);
            return defaultRoles;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load permissions configuration: " + configPath, ex);
        }
    }

    private boolean copyBundledDefault(Path target) throws IOException {
        try (InputStream bundled = getClass().getClassLoader().getResourceAsStream(DEFAULT_CONFIG_RESOURCE)) {
            if (bundled == null) {
                return false;
            }
            Files.copy(bundled, target);
            return true;
        }
    }

    private Map<String, StarryRole> buildRoles(StarryRoles config) {
        Map<String, StarryRole> resolvedRoles = new LinkedHashMap<>();

        StarryRole ownerStarryRole = new StarryRole(OWNER_ROLE_NAME, config.getDefaultColorPrefix());
        ownerStarryRole.permissions().grantAllAccess();
        resolvedRoles.put(OWNER_ROLE_NAME, ownerStarryRole);

        List<StarryRoles.StarryRole> configuredRoles = config.getAccounts() == null ? List.of() : config.getAccounts();
        for (StarryRoles.StarryRole configuredRole : configuredRoles) {
            String roleName = normalizeRoleName(configuredRole.getName());
            if (resolvedRoles.containsKey(roleName)) {
                throw new IllegalStateException("Duplicate role name in permissions configuration: " + roleName);
            }
            resolvedRoles.put(roleName, new StarryRole(roleName, configuredRole.getColorPrefix()));
        }

        for (StarryRoles.StarryRole configuredRole : configuredRoles) {
            registerReferencedPermissions(safeList(configuredRole.getPermissions()));
            registerReferencedPermissions(safeList(configuredRole.getRevokedPermissions()));
        }

        for (StarryRoles.StarryRole configuredRole : configuredRoles) {
            StarryRole starryRole = requireRole(resolvedRoles, configuredRole.getName());
            starryRole.permissions().merge(permissionResolver.resolveRules(safeList(configuredRole.getPermissions())));
            starryRole.revokedPermissions().merge(permissionResolver.resolveRules(safeList(configuredRole.getRevokedPermissions())));
        }

        for (StarryRoles.StarryRole configuredRole : configuredRoles) {
            StarryRole starryRole = requireRole(resolvedRoles, configuredRole.getName());
            for (String inheritedRoleName : safeList(configuredRole.getInherits())) {
                starryRole.inherit(requireRole(resolvedRoles, inheritedRoleName));
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

    private StarryRole requireRole(Map<String, StarryRole> roles, String roleName) {
        String normalizedRoleName = normalizeRoleName(roleName);
        StarryRole starryRole = roles.get(normalizedRoleName);
        if (starryRole == null) {
            throw new IllegalStateException("Unknown role in permissions configuration: " + normalizedRoleName);
        }
        return starryRole;
    }

    private void registerReferencedPermissions(List<String> permissionRules) {
        for (String permissionRule : permissionRules) {
            if (permissionRule != null
                    && !permissionRule.isBlank()
                    && !permissionRule.endsWith("*")
                    && !PermissionRegistry.contains(permissionRule)) {
                PermissionRegistry.registerIfAbsent(permissionRule);
            }
        }
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
