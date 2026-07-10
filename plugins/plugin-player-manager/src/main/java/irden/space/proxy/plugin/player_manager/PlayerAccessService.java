package irden.space.proxy.plugin.player_manager;

import irden.space.proxy.plugin.api.*;
import irden.space.proxy.plugin.api.annotations.OnLoad;
import irden.space.proxy.plugin.api.annotations.RegisterPluginPermissions;
import irden.space.proxy.plugin.player_manager.model.Player;
import irden.space.proxy.plugin.player_manager.model.StarryRole;
import irden.space.proxy.plugin.player_manager.model.UserPermissions;
import irden.space.proxy.plugin.player_manager.permissions.PermissionResolver;
import irden.space.proxy.plugin.player_manager.persistence.PlayerAccessJdbcRepository;
import irden.space.proxy.plugin.player_manager.persistence.model.PlayerRoleRecord;
import irden.space.proxy.plugin.player_manager.roles.RoleManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@Component
@RequiredArgsConstructor
public class PlayerAccessService {

    private final Map<String, StarryRole> rolesByName = new ConcurrentHashMap<>();

    private final PlayerAccessJdbcRepository playerAccessRepository;
    private final SessionPermissionService sessionPermissionService;
    private final RoleManager roleManager;
    private final PermissionResolver permissionResolver;
    private final PlayerDirectory playerDirectory;

    @SuppressWarnings("unused")
    @RegisterPluginPermissions
    public List<Class<? extends PermissionEnum>> registerPermissions() {
        return List.of(PlayerManagerPermissions.class);
    }

    @OnLoad
    public void reloadRolesOnLoad() {
        reloadConfiguredRoles();
    }

    public Optional<StarryRole> findRole(String roleName) {
        if (roleName == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(rolesByName.get(roleName));
    }

    /**
     * Resolves and binds the effective permissions for a freshly connected session.
     */
    public void applyResolvedPermissions(String sessionId, String playerUuid, String accountName) {
        ResolvedUserAccess resolvedUserAccess = resolveUserAccess(playerUuid, accountName);
        bindSessionPermissions(
                sessionId,
                resolvedUserAccess.starryRoles(),
                resolvedUserAccess.grantedPermissions(),
                resolvedUserAccess.revokedPermissions()
        );
    }


    public void bindSessionPermissionsByRoleNames(String sessionId, List<String> roleNames, List<String> extraPermissionRules) {
        bindSessionPermissions(sessionId, resolveRoles(roleNames), extraPermissionRules);
    }

    public void bindSessionPermissions(String sessionId, List<StarryRole> starryRoles, List<String> extraPermissionRules) {
        bindSessionPermissions(sessionId, starryRoles, permissionResolver.resolveRules(extraPermissionRules));
    }

    public void bindSessionPermissions(String sessionId, List<StarryRole> starryRoles, PermissionSet extraPermissions) {
        bindSessionPermissions(sessionId, starryRoles, extraPermissions, Permissions.none());
    }

    public void bindSessionPermissions(String sessionId, List<StarryRole> starryRoles, PermissionSet grantedPermissions, PermissionSet revokedPermissions) {
        sessionPermissionService.updatePermissions(sessionId, new UserPermissions(starryRoles, grantedPermissions, revokedPermissions));
    }

    public PermissionSet resolvePermissions(List<String> permissionRules) {
        return permissionResolver.resolveRules(permissionRules);
    }

    public void assignRoleToPlayer(String playerUuid, String roleName, String assignedBy) {
        ensurePlayerAccessMutable(playerUuid);

        if (RoleManager.OWNER_ROLE_NAME.equals(roleName)) {
            throw new IllegalArgumentException("Owner role is managed internally and cannot be assigned manually");
        }

        requireRole(roleName);
        playerAccessRepository.assignRole(playerUuid, roleName, assignedBy);
        refreshOnlinePermissions(playerUuid);
    }

    public void removeRoleFromPlayer(String playerUuid, String roleName) {
        ensurePlayerAccessMutable(playerUuid);

        if (RoleManager.OWNER_ROLE_NAME.equals(roleName)) {
            throw new IllegalArgumentException("Owner role cannot be removed manually");
        }

        playerAccessRepository.removeRole(playerUuid, roleName);
        refreshOnlinePermissions(playerUuid);
    }

    public void grantPermissionToPlayer(String playerUuid, String permissionRule, String changedBy) {
        ensurePlayerAccessMutable(playerUuid);
        playerAccessRepository.savePermissionOverride(playerUuid, normalizePermissionRule(permissionRule), true, changedBy);
        refreshOnlinePermissions(playerUuid);
    }

    public void revokePermissionFromPlayer(String playerUuid, String permissionRule, String changedBy) {
        ensurePlayerAccessMutable(playerUuid);
        playerAccessRepository.savePermissionOverride(playerUuid, normalizePermissionRule(permissionRule), false, changedBy);
        refreshOnlinePermissions(playerUuid);
    }

    public void clearPermissionOverride(String playerUuid, String permissionRule) {
        ensurePlayerAccessMutable(playerUuid);
        playerAccessRepository.deletePermissionOverride(playerUuid, normalizePermissionRule(permissionRule));
        refreshOnlinePermissions(playerUuid);
    }

    public List<String> listEffectivePermissionNames(Player player) {
        PermissionView permissionView = player.permissions();
        return PermissionRegistry.entries().entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.naturalOrder()))
                .filter(entry -> permissionView.has(entry.getValue()))
                .map(Map.Entry::getKey)
                .toList();
    }

    private List<StarryRole> resolveRoles(List<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return List.of();
        }

        List<StarryRole> resolvedStarryRoles = new ArrayList<>(roleNames.size());
        for (String roleName : roleNames) {
            resolvedStarryRoles.add(requireRole(roleName));
        }

        return List.copyOf(resolvedStarryRoles);
    }

    private StarryRole requireRole(String roleName) {
        return findRole(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Unknown role: " + roleName));
    }

    private void reloadConfiguredRoles() {
        rolesByName.clear();
        rolesByName.putAll(roleManager.rolesByName());
    }

    private ResolvedUserAccess resolveUserAccess(String playerUuid, String accountName) {
        if (roleManager.isOwner(playerUuid)) {
            return new ResolvedUserAccess(resolveRoles(List.of(RoleManager.OWNER_ROLE_NAME)), Permissions.none(), Permissions.none());
        }

        List<String> storedRoleNames = playerAccessRepository.findRolesByPlayerUuid(playerUuid).stream()
                .map(PlayerRoleRecord::roleName)
                .toList();

        List<StarryRole> resolvedStarryRoles = resolveRoles(roleManager.resolveRoleNamesForPlayer(playerUuid, accountName, storedRoleNames));

        PermissionSet grantedPermissions = new PermissionSet();
        PermissionSet revokedPermissions = new PermissionSet();
        for (var permissionOverride : playerAccessRepository.findPermissionOverridesByPlayerUuid(playerUuid)) {
            mergePermissionRule(
                    permissionOverride.permissionName(),
                    permissionOverride.granted() ? grantedPermissions : revokedPermissions
            );
        }

        return new ResolvedUserAccess(resolvedStarryRoles, grantedPermissions, revokedPermissions);
    }

    private void mergePermissionRule(String permissionRule, PermissionSet targetPermissions) {
        String normalizedPermissionRule = normalizePermissionRule(permissionRule);
        if (!normalizedPermissionRule.endsWith("*")) {
            PermissionRegistry.registerIfAbsent(normalizedPermissionRule);
        }
        targetPermissions.merge(permissionResolver.resolveRule(normalizedPermissionRule));
    }

    private String normalizePermissionRule(String permissionRule) {
        if (permissionRule == null || permissionRule.isBlank()) {
            throw new IllegalArgumentException("Permission rule must not be blank");
        }
        return permissionRule.trim();
    }

    private void ensurePlayerAccessMutable(String playerUuid) {
        if (roleManager.isOwner(playerUuid)) {
            throw new IllegalStateException("Owner roles and permissions cannot be modified");
        }
    }

    private void refreshOnlinePermissions(String playerUuid) {
        playerDirectory.getPlayerByUuid(playerUuid, true).ifPresent(player -> {
            ResolvedUserAccess resolvedUserAccess = resolveUserAccess(playerUuid, player.account());
            bindSessionPermissions(
                    player.sessionContext().sessionId(),
                    resolvedUserAccess.starryRoles(),
                    resolvedUserAccess.grantedPermissions(),
                    resolvedUserAccess.revokedPermissions()
            );
        });
    }

    private record ResolvedUserAccess(List<StarryRole> starryRoles, PermissionSet grantedPermissions,
                                      PermissionSet revokedPermissions) {
    }
}
