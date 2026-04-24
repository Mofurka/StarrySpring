package irden.space.proxy.plugin.player_manager;

import irden.space.proxy.plugin.api.*;
import irden.space.proxy.plugin.api.annotations.OnLoad;
import irden.space.proxy.plugin.api.annotations.OnStart;
import irden.space.proxy.plugin.api.annotations.OnStop;
import irden.space.proxy.plugin.api.annotations.PacketHandler;
import irden.space.proxy.plugin.player_manager.model.Player;
import irden.space.proxy.plugin.player_manager.model.TempPlayer;
import irden.space.proxy.plugin.player_manager.persistence.LiquibaseRunner;
import irden.space.proxy.plugin.player_manager.persistence.PlayerJdbcRepository;
import irden.space.proxy.plugin.player_manager.persistence.model.PlayerRecord;
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
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

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
    private PlayerJdbcRepository playerRepository;


    @OnLoad
    public void handleLoad(PluginContext context) {
        log.info("Loading plugin '{}'", descriptor().id());
        context.publishService(PlayerManagerPlugin.class, this);
        DataSource dataSource = context.requireService(DataSource.class);
        LiquibaseRunner.runLiquibaseMigrations(dataSource);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        this.playerRepository = new PlayerJdbcRepository(jdbcTemplate);
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
            log.info("New player detected: name='{}', uuid={}, ip={}", clientConnect.playerName(), clientConnect.playerUuid(), context.session().clientIp());
            playerRepository.save(PlayerRecord.builder()
                    .playerUuid(clientConnect.playerUuid().toString())
                    .name(clientConnect.playerName())
                    .ipAddress(context.session().clientIp())
                    .createdAt(LocalDateTime.now())
                    .build());
        } else {
            log.info("Existing player connecting: name='{}', uuid={}, ip={}", clientConnect.playerName(), clientConnect.playerUuid(), context.session().clientIp());
            playerRepository.updatePlayerIpAddress(clientConnect.playerUuid().toString(), context.session().clientIp());
        }

        TempPlayer player = TempPlayer.builder()
                .name(clientConnect.playerName())
                .uuid(clientConnect.playerUuid())
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
                    .sessionContext(context.session())
                    .ipAddress(context.session().clientIp())
                    .clientId(connectSuccess.clientId())
                    .entityId(connectSuccess.clientId() * -65536) // This how clientId transforms to entityId.
                    .build();
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
            return playerRecordOpt.map(record -> Player.builder()
                    .name(record.name())
                    .uuid(StarUuid.fromHex(record.playerUuid()))
                    .ipAddress(record.ipAddress())
                    .build());
        }
    }
}
