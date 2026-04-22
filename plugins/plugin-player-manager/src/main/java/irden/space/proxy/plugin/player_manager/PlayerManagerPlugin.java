package irden.space.proxy.plugin.player_manager;

import irden.space.proxy.plugin.api.*;
import irden.space.proxy.plugin.api.annotations.OnLoad;
import irden.space.proxy.plugin.api.annotations.OnStart;
import irden.space.proxy.plugin.api.annotations.OnStop;
import irden.space.proxy.plugin.api.annotations.PacketHandler;
import irden.space.proxy.plugin.command_handler.ChatCommand;
import irden.space.proxy.plugin.command_handler.CommandContext;
import irden.space.proxy.plugin.command_handler.color.Color;
import irden.space.proxy.plugin.player_manager.model.Player;
import irden.space.proxy.plugin.player_manager.model.TempPlayer;
import irden.space.proxy.plugin.player_manager.persistence.PlayerJdbcRepository;
import irden.space.proxy.plugin.player_manager.persistence.PlayerRecord;
import irden.space.proxy.protocol.codec.variant.StringVariantValue;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketType;
import irden.space.proxy.protocol.payload.packet.client_connect.ClientConnect;
import irden.space.proxy.protocol.payload.packet.connect.ConnectFailure;
import irden.space.proxy.protocol.payload.packet.connect.ConnectSuccess;
import irden.space.proxy.protocol.payload.packet.entity.type.Entity;
import irden.space.proxy.protocol.payload.packet.entity.type.PlayerEntity;
import irden.space.proxy.protocol.payload.packet.entity.type.player.PlayerNetState;
import irden.space.proxy.protocol.payload.packet.entity.update.EffectsAnimator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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

        DataSource dataSource = context.requireService(DataSource.class);

        PluginLiquibaseRunner.run(dataSource, "db/changelog/player-manager.xml");

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
            playerRepository.save(PlayerRecord.builder()
                    .id(UUID.randomUUID())
                    .playerUuid(player.uuid().toString())
                    .name(player.name())
                    .ipAddress(player.ipAddress())
                    .createdAt(LocalDateTime.now())
                    .build());
            return PacketDecision.forward();
        }
        declineConnection(context.session(), "Player connection state not found. Please try again.");
        return PacketDecision.cancel();
    }

    private void declineConnection(PluginSessionContext context, String reason) {
        ConnectFailure connectFailure = new ConnectFailure(reason);
        context.sendToClient(PacketType.CONNECT_FAILURE, connectFailure);
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

    @PacketHandler(value = PacketType.ENTITY_CREATE, direction = PacketDirection.TO_CLIENT)
    public PacketDecision onEntityCreate(PacketInterceptionContext context) {
        Entity entity = (Entity) context.parsedPayload();
        if (entity instanceof PlayerEntity playerEntity) {
            Player player = players.getBySessionId(context.session().sessionId());
            if (playerEntity.entityId() != player.entityId() && player.entityId() != 0) {
                try {
                    return PacketDecision.forward();
                } finally {
                    CompletableFuture.runAsync(() -> sendEntityNames(context.session(), playerEntity.entityId()));
                }
            }


        }
        return PacketDecision.forward();
    }

    private void sendEntityNames(PluginSessionContext session, int entityId) {
        Thread.yield(); // Ensure this runs after the original ENTITY_CREATE packet is processed
        try {
            Thread.sleep(10); // Small delay to ensure the client has processed the ENTITY_CREATE packet
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while waiting to send nametag update for entityId={}", entityId);
        }
        var player = PlayerNetState.builder();
        var effectsAnimator = EffectsAnimator.builder();
        int connectionId = entityId / -65536;
        player.connectionId(connectionId);
        player.entityId(entityId);
        String name = "??? " + Color.CYAN.colorString("[%s]".formatted(connectionId), true);
        effectsAnimator.globalTags(Map.of("nametag", new StringVariantValue(name)));
        player.effectsAnimator(effectsAnimator.build());
        PlayerNetState build = player.build();
        log.info("Sending nametag update for entityId={} (connectionId={})", entityId, connectionId);
        for (int i = 0; i < 3; i++) {
            session.sendToClient(PacketType.ENTITY_UPDATE, build);
        }
    }

    @ChatCommand(value = "setnametag",
            aliases = {"sn"},
            usage = "<connectionId> <nametag>")
    public void setNametagCommand(CommandContext context) {
        if (context.arguments().size() < 2) {
            context.reply("Usage: /setnametag <connectionId> <nametag>");
            return;
        }
        String connectionIdStr = context.arguments().getFirst();
        String nametag = context.arguments().get(1);
        int entityId = Integer.parseInt(connectionIdStr) * -65536;
        log.info("Setting nametag for connectionId={} (entityId={}) to '{}'", connectionIdStr, entityId, nametag);
        var player = PlayerNetState.builder();
        var effectsAnimator = EffectsAnimator.builder();
        nametag = nametag + " " + Color.CYAN.colorString("[%s]".formatted(connectionIdStr), true);
        effectsAnimator.globalTags(Map.of("nametag", new StringVariantValue(nametag)));
        player.effectsAnimator(effectsAnimator.build());
        player.entityId(entityId);
        player.connectionId(Integer.parseInt(connectionIdStr));
        context.session().sendToClient(PacketType.ENTITY_UPDATE, player.build());
        context.reply("Nametag update sent to connectionId=" + connectionIdStr);
    }

}
