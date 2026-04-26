package irden.space.proxy.plugin.player_manager;

import irden.space.proxy.plugin.api.*;
import irden.space.proxy.plugin.api.annotations.OnLoad;
import irden.space.proxy.plugin.api.annotations.OnStart;
import irden.space.proxy.plugin.api.annotations.OnStop;
import irden.space.proxy.plugin.api.annotations.PacketHandler;
import irden.space.proxy.plugin.command_handler.*;
import irden.space.proxy.plugin.player_manager.model.Player;
import irden.space.proxy.plugin.player_manager.model.Role;
import irden.space.proxy.plugin.player_manager.model.TempPlayer;
import irden.space.proxy.plugin.player_manager.model.UserPermissions;
import irden.space.proxy.plugin.player_manager.permissions.PermissionResolver;
import irden.space.proxy.plugin.player_manager.persistence.LiquibaseRunner;
import irden.space.proxy.plugin.player_manager.persistence.PlayerAccessJdbcRepository;
import irden.space.proxy.plugin.player_manager.persistence.PlayerJdbcRepository;
import irden.space.proxy.plugin.player_manager.persistence.model.PlayerRecord;
import irden.space.proxy.plugin.player_manager.roles.RoleActionType;
import irden.space.proxy.plugin.player_manager.roles.RoleManager;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketType;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;
import irden.space.proxy.protocol.payload.packet.client_connect.ClientConnect;
import irden.space.proxy.protocol.payload.packet.connect.ConnectFailure;
import irden.space.proxy.protocol.payload.packet.connect.ConnectSuccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static irden.space.proxy.plugin.command_handler.CommandSpec.argument;
import static irden.space.proxy.plugin.command_handler.CommandSpec.literal;
import static java.lang.Boolean.FALSE;

@PluginDefinition(
        id = "player-manager",
        name = "Player Manager",
        version = "1.0.0",
        dependsOn = {"command-handler"},
        author = "https://github.com/Mofurka",
        description = "Plugin for player managing."
)
public final class PlayerManagerPlugin implements ProxyPlugin {
    private static final Logger log = LoggerFactory.getLogger(PlayerManagerPlugin.class);
    private final PlayerRegistry<Player> players = new InMemoryPlayerRegistry();
    private final PlayerRegistry<TempPlayer> connectingPlayers = new InMemoryConnectingPlayers();
    private final Map<String, Role> rolesByName = new ConcurrentHashMap<>();
    private final PermissionResolver permissionResolver = new PermissionResolver();
    private PlayerAccessJdbcRepository playerAccessRepository;
    private PlayerJdbcRepository playerRepository;
    private SessionPermissionService sessionPermissionService;
    private RoleManager roleManager;


    @OnLoad
    public void handleLoad(PluginContext context) {
        log.info("Loading plugin '{}'", descriptor().id());
        ChatPermissions.registerDefaults();
        context.publishService(PlayerManagerPlugin.class, this);
        DataSource dataSource = context.requireService(DataSource.class);
        this.sessionPermissionService = context.requireService(SessionPermissionService.class);
        LiquibaseRunner.runLiquibaseMigrations(dataSource);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        this.playerAccessRepository = new PlayerAccessJdbcRepository(jdbcTemplate);
        this.playerRepository = new PlayerJdbcRepository(jdbcTemplate);
        this.roleManager = new RoleManager();
        reloadConfiguredRoles();
    }

    @OnStart
    public void handleStart() {
        log.info("Started plugin '{}'", descriptor().id());
    }

    @OnStop
    public void handleStop() {
        log.info("Stopped plugin '{}'", descriptor().id());
    }


    @PacketHandler(value = PacketType.CLIENT_CONNECT, direction = PacketDirection.TO_SERVER)
    public PacketDecision onClientConnect(PacketInterceptionContext context) {
        ClientConnect clientConnect = (ClientConnect) context.parsedPayload();
        if (playerRepository.findByUuid(clientConnect.playerUuid().toString()).isEmpty()) {
            if (log.isInfoEnabled())
                log.info("New player detected: name='{}', uuid={}, ip={}", clientConnect.playerName(), clientConnect.playerUuid(), context.session().clientIp());
            playerRepository.save(PlayerRecord.builder()
                    .playerUuid(clientConnect.playerUuid().toString())
                    .name(clientConnect.playerName())
                    .ipAddress(context.session().clientIp())
                    .createdAt(LocalDateTime.now())
                    .build());
        } else {
            if (log.isInfoEnabled())
                log.info("Existing player connecting: name='{}', uuid={}, ip={}", clientConnect.playerName(), clientConnect.playerUuid(), context.session().clientIp());
            playerRepository.updatePlayerIpAddress(clientConnect.playerUuid().toString(), context.session().clientIp());
        }
        TempPlayer player = TempPlayer.builder()
                .name(clientConnect.playerName())
                .uuid(clientConnect.playerUuid())
                .account(clientConnect.account())
                .sessionId(context.session().sessionId()).build();
        connectingPlayers.add(context.session().sessionId(), player);
        return PacketDecision.forward();
    }

    // This packet is sent by the server when the client successfully connects. We can use it to confirm player connections and log additional info.
    @PacketHandler(value = PacketType.CONNECT_SUCCESS, direction = PacketDirection.TO_CLIENT)
    public PacketDecision onConnectSuccess(PacketInterceptionContext context) {
        ConnectSuccess connectSuccess = (ConnectSuccess) context.parsedPayload();
        TempPlayer tempPlayer = connectingPlayers.removeBySessionId(context.session().sessionId());
        if (tempPlayer != null) {
            Player player = Player.builder()
                    .name(tempPlayer.name())
                    .uuid(tempPlayer.uuid())
                    .account(tempPlayer.account())
                    .sessionContext(context.session())
                    .ipAddress(context.session().clientIp())
                    .clientId(connectSuccess.clientId())
                    .entityId(connectSuccess.clientId() * -65536) // This how clientId transforms to entityId.
                    .build();
            ResolvedUserAccess resolvedUserAccess = resolveUserAccess(tempPlayer.uuid().toString(), tempPlayer.account());
            bindSessionPermissions(
                    context.session().sessionId(),
                    resolvedUserAccess.roles(),
                    resolvedUserAccess.grantedPermissions(),
                    resolvedUserAccess.revokedPermissions()
            );
            log.info(
                    "Player connected: name='{}', uuid={}, clientId={}, entityId={}",
                    player.name(),
                    player.uuid(),
                    player.clientId(),
                    player.entityId()
            );
            players.add(context.session().sessionId(), player);

            return PacketDecision.forward();
        }
        ConnectFailure connectFailure = new ConnectFailure("Player connection state not found. Please try again.");
        context.session().sendToClient(PacketType.CONNECT_FAILURE, connectFailure);
        return PacketDecision.cancel();
    }

    // Тест отправки сообщений с проверкой прав
    @PacketHandler(value = PacketType.CHAT_SENT, direction = PacketDirection.TO_SERVER)
    public PacketDecision onChatSent(PacketInterceptionContext context) {
        if (log.isDebugEnabled() && !Permissions.has(context.session(), ChatPermissions.SENT.permission())) {
            log.debug("Session {} attempted to send chat without '{}' permission", context.session().sessionId(), ChatPermissions.SENT.name());
        }
        return PacketDecision.forward();
    }


    @Override
    public void onConnectionSuccess(PluginSessionContext context) {
        if (log.isInfoEnabled()) log.info("Session {} connected successfully.", context.sessionId());
    }

    @Override
    public void onDisconnecting(PluginSessionContext context) {
        if (log.isInfoEnabled()) log.info("Session {} is disconnecting.", context.sessionId());
    }

    @Override
    public void onDisconnected(PluginSessionContext context) {
        if (log.isInfoEnabled()) log.info("Session {} has disconnected.", context.sessionId());
        sessionPermissionService.clearPermissions(context.sessionId());
        TempPlayer tempPlayer = connectingPlayers.removeBySessionId(context.sessionId());
        if (tempPlayer != null) {
            log.info("Player connection attempt failed or was cancelled: name='{}', uuid={}", tempPlayer.name(), tempPlayer.uuid());
            return;
        }
        Player player = players.removeBySessionId(context.sessionId());
        if (player != null) {
            log.info("Player disconnected: name='{}', uuid={}", player.name(), player.uuid());
        }
    }

    public Optional<Role> findRole(String roleName) {
        if (roleName == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(rolesByName.get(roleName));
    }

    public void bindSessionPermissionsByRoleNames(String sessionId, List<String> roleNames, List<String> extraPermissionRules) {
        bindSessionPermissions(sessionId, resolveRoles(roleNames), extraPermissionRules);
    }

    public void bindSessionPermissions(String sessionId, List<Role> roles, List<String> extraPermissionRules) {
        bindSessionPermissions(sessionId, roles, permissionResolver.resolveRules(extraPermissionRules));
    }

    public void bindSessionPermissions(String sessionId, List<Role> roles, PermissionSet extraPermissions) {
        bindSessionPermissions(sessionId, roles, extraPermissions, Permissions.none());
    }

    public void bindSessionPermissions(String sessionId, List<Role> roles, PermissionSet grantedPermissions, PermissionSet revokedPermissions) {
        sessionPermissionService.updatePermissions(sessionId, new UserPermissions(roles, grantedPermissions, revokedPermissions));
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

    private List<Role> resolveRoles(List<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return List.of();
        }

        List<Role> resolvedRoles = new ArrayList<>(roleNames.size());
        for (String roleName : roleNames) {
            resolvedRoles.add(requireRole(roleName));
        }

        return List.copyOf(resolvedRoles);
    }

    private Role requireRole(String roleName) {
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
                .map(irden.space.proxy.plugin.player_manager.persistence.model.PlayerRoleRecord::roleName)
                .toList();

        List<Role> resolvedRoles = resolveRoles(roleManager.resolveRoleNamesForPlayer(playerUuid, accountName, storedRoleNames));

        PermissionSet grantedPermissions = new PermissionSet();
        PermissionSet revokedPermissions = new PermissionSet();
        for (var permissionOverride : playerAccessRepository.findPermissionOverridesByPlayerUuid(playerUuid)) {
            mergePermissionRule(
                    permissionOverride.permissionName(),
                    permissionOverride.granted() ? grantedPermissions : revokedPermissions
            );
        }

        return new ResolvedUserAccess(resolvedRoles, grantedPermissions, revokedPermissions);
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
        getPlayerByUuid(playerUuid, true).ifPresent(player -> {
            ResolvedUserAccess resolvedUserAccess = resolveUserAccess(playerUuid, player.account());
            bindSessionPermissions(
                    player.sessionContext().sessionId(),
                    resolvedUserAccess.roles(),
                    resolvedUserAccess.grantedPermissions(),
                    resolvedUserAccess.revokedPermissions()
            );
        });
    }

    @ChatCommand(value = "user",
            aliases = {"u"},
            description = "use for manage players")
    public CommandSpec userCommand() {
        return literal("user")
                .then(argument("identifier", StringArgumentType.word()).description("Player name, UUID or client ID")
                        .then(literal("info")
                                .executes(context -> {
                                    String identifier = context.get("identifier", String.class);
                                    Optional<Player> playerOpt = findPlayer(identifier, false);
                                    if (playerOpt.isEmpty()) {
                                        context.reply("Player not found: " + identifier);
                                        return;
                                    }
                                    StringBuilder sb = new StringBuilder();
                                    Player player = playerOpt.get();
                                    sb.append("Player info:").append(System.lineSeparator());
                                    sb.append("- Name: ").append(player.name()).append(System.lineSeparator());
                                    sb.append("- UUID: ").append(player.uuid()).append(System.lineSeparator());
                                    sb.append("- Online: ").append(player.online()).append(System.lineSeparator());
                                    if (player.online()) {
                                        sb.append("- Account: ").append(player.account()).append(System.lineSeparator());
                                        sb.append("- Client ID: ").append(player.clientId()).append(System.lineSeparator());
                                        sb.append("- Entity ID: ").append(player.entityId()).append(System.lineSeparator());
                                        sb.append("- IP Address: ").append(player.ipAddress()).append(System.lineSeparator());
                                    }
                                    context.reply(sb.toString());
                                }))
                        .then(literal("permissions")
                                .executes(context -> {
                                    String identifier = context.get("identifier", String.class);
                                    Optional<Player> playerOpt = findPlayer(identifier, false);
                                    if (playerOpt.isEmpty()) {
                                        context.reply("Player not found: " + identifier);
                                        return;
                                    }
                                    Player player = playerOpt.get();
                                    StringBuilder sb = new StringBuilder();
                                    sb.append("Player permissions:").append(System.lineSeparator());
                                    sb.append("- Name: ").append(player.name()).append(System.lineSeparator());
                                    sb.append("- UUID: ").append(player.uuid()).append(System.lineSeparator());

                                    List<String> permissionNames = listEffectivePermissionNames(player);
                                    if (permissionNames.isEmpty()) {
                                        sb.append("- Effective permissions: none").append(System.lineSeparator());
                                        context.reply(sb.toString());
                                        return;
                                    }

                                    sb.append("- Effective permissions (").append(permissionNames.size()).append("):").append(System.lineSeparator());
                                    for (String permissionName : permissionNames) {
                                        sb.append("  - ").append(permissionName).append(System.lineSeparator());
                                    }
                                    context.reply(sb.toString());
                                })
                        )
                        .then(literal("role")
                                .then(argument("action", EnumArgumentType.of(RoleActionType.class))
                                        .then(argument("roles", StringArgumentType.greedyString()).description("Role names separated by space")
                                                .executes(this::handlePlayerRoleUpdate)))
                        )
                ).build();
    }


    private void handlePlayerRoleUpdate(CommandContext ctx) {
        String identifier = ctx.get("identifier", String.class);
        Optional<Player> playerOpt = findPlayer(identifier, false);
        if (playerOpt.isEmpty()) {
            ctx.reply("Player not found: " + identifier);
            return;
        }
        Player player = playerOpt.get();
        RoleActionType actionType = ctx.get("action", RoleActionType.class);
        String[] roleNames = Optional.ofNullable(ctx.get("roles", String.class))
                .map(r -> r.split("\\s+"))
                .orElse(new String[0]);
        var sb = new StringBuilder();
        for (int i = 0; i < roleNames.length; i++) {
            roleNames[i] = roleNames[i].trim();
            try {
                switch (actionType) {
                    case ADD -> {
                        assignRoleToPlayer(player.uuid().toString(), roleNames[i], "console");
                        sb.append("Assigned role '%s' to player '%s'%n".formatted(roleNames[i], player.name()));
                    }
                    case REMOVE -> {
                        removeRoleFromPlayer(player.uuid().toString(), roleNames[i]);
                        sb.append("Removed role '%s' from player '%s'%n".formatted(roleNames
                                [i], player.name()));
                    }
                }
            } catch (Exception e) {
                sb.append("Failed to %s role '%s' for player '%s': %s%n".formatted(
                        actionType == RoleActionType.ADD ? "assign" : "remove",
                        roleNames[i],
                        player.name(),
                        e.getMessage()
                ));
                ctx.reply(sb.toString());
                return;
            }
        }
        ctx.reply(sb.toString());
    }
//    @PacketHandler(value = PacketType.ENTITY_CREATE, direction = PacketDirection.TO_CLIENT)
//    public PacketDecision onEntityCreate(PacketInterceptionContext context) {
//        Entity entity = (Entity) context.parsedPayload();
//        if (entity instanceof PlayerEntity playerEntity) {
//            Player player = players.getBySessionId(context.session().sessionId());
//            if (playerEntity.entityId() != player.entityId() && player.entityId() != 0) {
//                try {
//                    return PacketDecision.forward();
//                } finally {
//                    CompletableFuture.runAsync(() -> sendEntityNames(context.session(), playerEntity.entityId()));
//                }
//            }
//
//
//        }
//        return PacketDecision.forward();
//    }
//
//    private void sendEntityNames(PluginSessionContext session, int entityId) {
//        Thread.yield(); // Ensure this runs after the original ENTITY_CREATE packet is processed
//        try {
//            Thread.sleep(10); // Small delay to ensure the client has processed the ENTITY_CREATE packet
//        } catch (InterruptedException _) {
//            Thread.currentThread().interrupt();
//            log.warn("Interrupted while waiting to send nametag update for entityId={}", entityId);
//        }
//        var player = PlayerNetState.builder();
//        var effectsAnimator = EffectsAnimator.builder();
//        int connectionId = entityId / -65536;
//        player.connectionId(connectionId);
//        player.entityId(entityId);
//        String name = "??? " + Color.CYAN.colorString("[%s]".formatted(connectionId), true);
//        effectsAnimator.globalTags(Map.of("nametag", new StringVariantValue(name)));
//        player.effectsAnimator(effectsAnimator.build());
//        PlayerNetState build = player.build();
//        log.info("Sending nametag update for entityId={} (connectionId={})", entityId, connectionId);
//        for (int i = 0; i < 3; i++) {
//            session.sendToClient(PacketType.ENTITY_UPDATE, build);
//        }
//    }
//
//    @ChatCommand(value = "setnametag",
//            aliases = {"sn"},
//            usage = "<connectionId> <nametag>")
//    public void setNametagCommand(CommandContext context) {
//        if (context.arguments().size() < 2) {
//            context.reply("Usage: /setnametag <connectionId> <nametag>");
//            return;
//        }
//        String connectionIdStr = context.arguments().getFirst();
//        String nametag = context.arguments().get(1);
//        int entityId = Integer.parseInt(connectionIdStr) * -65536;
//        log.info("Setting nametag for connectionId={} (entityId={}) to '{}'", connectionIdStr, entityId, nametag);
//        var player = PlayerNetState.builder();
//        var effectsAnimator = EffectsAnimator.builder();
//        nametag = nametag + " " + Color.CYAN.colorString("[%s]".formatted(connectionIdStr), true);
//        effectsAnimator.globalTags(Map.of("nametag", new StringVariantValue(nametag)));
//        player.effectsAnimator(effectsAnimator.build());
//        player.entityId(entityId);
//        player.connectionId(Integer.parseInt(connectionIdStr));
//        context.session().sendToClient(PacketType.ENTITY_UPDATE, player.build());
//        context.reply("Nametag update sent to connectionId=" + connectionIdStr);
//    }


    public Optional<Player> findPlayer(String identifier, boolean loggedIn) {
        var isStarUuid = identifier.matches("^[0-9a-fA-F]{32}$");
        var isClientId = identifier.matches("^\\d+$");
        Optional<Player> first = players.getAll().stream()
                .filter(p -> p.name().equals(identifier) ||
                        (isStarUuid && p.uuid().toString().equals(identifier)) ||
                        (isClientId && Integer.toString(p.clientId()).equals(identifier)))
                .findFirst();
        if (first.isPresent()) {
            return first;
        }
        if (!loggedIn) {
            Optional<PlayerRecord> playerRecordOpt = playerRepository.findByName(identifier);
            if (playerRecordOpt.isEmpty()) {
                playerRecordOpt = playerRepository.findByUuid(identifier);
            }
            return playerRecordOpt.map(p -> Player.builder()
                    .name(p.name())
                    .uuid(StarUuid.fromHex(p.playerUuid()))
                    .ipAddress(p.ipAddress())
                    .build());
        }
        return Optional.empty();
    }


    public List<Player> findAllPlayersByIpAddress(String ipAddress) {
        List<Player> onlinePlayers = players.getAll().stream()
                .filter(p -> p.ipAddress().equals(ipAddress))
                .toList();
        List<Player> offlinePlayers = playerRepository.findByIpAddress(ipAddress).stream()
                .map(p -> Player.builder()
                        .name(p.name())
                        .uuid(StarUuid.fromHex(p.playerUuid()))
                        .ipAddress(p.ipAddress())
                        .build())
                .toList();
        return Stream.concat(onlinePlayers.stream(), offlinePlayers.stream())
                .distinct()
                .toList();
    }


    public Optional<Player> getPlayerByClientId(int clientId) {
        return players.getAll().stream()
                .filter(p -> p.clientId() == clientId)
                .findFirst();
    }


    public Optional<Player> getPlayerBySessionId(String sessionId) {
        Player bySessionId = players.getBySessionId(sessionId);
        if (bySessionId != null) {
            return Optional.of(bySessionId);
        }
        return Optional.empty();
    }

    public Optional<Player> getPlayerByName(String name, boolean loogedIn) {
        if (loogedIn) {
            return players.getAll().stream()
                    .filter(p -> p.name().equals(name))
                    .findFirst();
        } else {
            Optional<PlayerRecord> playerRecordOpt = playerRepository.findByName(name);
            return playerRecordOpt.map(playerRecord -> Player.builder()
                    .name(playerRecord.name())
                    .uuid(StarUuid.fromHex(playerRecord.playerUuid()))
                    .ipAddress(playerRecord.ipAddress())
                    .build());
        }
    }


    public Optional<Player> getPlayerByUuid(String uuid, boolean loogedIn) {
        if (loogedIn) {
            return players.getAll().stream()
                    .filter(p -> p.uuid().toString().equals(uuid))
                    .findFirst();
        } else {
            Optional<PlayerRecord> playerRecordOpt = playerRepository.findByUuid(uuid);
            return playerRecordOpt.map(p -> Player.builder()
                    .name(p.name())
                    .uuid(StarUuid.fromHex(p.playerUuid()))
                    .ipAddress(p.ipAddress())
                    .build());
        }
    }

    private List<String> listEffectivePermissionNames(Player player) {
        PermissionView permissionView = player.permissions();
        return PermissionRegistry.entries().entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.naturalOrder()))
                .filter(entry -> permissionView.has(entry.getValue()))
                .map(Map.Entry::getKey)
                .toList();
    }

    private record ResolvedUserAccess(List<Role> roles, PermissionSet grantedPermissions,
                                      PermissionSet revokedPermissions) {
    }
}
