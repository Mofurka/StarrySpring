package irden.space.proxy.plugin.player_manager;

import irden.space.proxy.plugin.api.PacketDecision;
import irden.space.proxy.plugin.api.PacketInterceptionContext;
import irden.space.proxy.plugin.api.PluginSessionContext;
import irden.space.proxy.plugin.api.SessionPermissionService;
import irden.space.proxy.plugin.api.annotations.OnConnectionSuccess;
import irden.space.proxy.plugin.api.annotations.OnDisconnected;
import irden.space.proxy.plugin.api.annotations.OnDisconnecting;
import irden.space.proxy.plugin.api.annotations.PacketHandler;
import irden.space.proxy.plugin.player_manager.events.PlayerConnectedEvent;
import irden.space.proxy.plugin.player_manager.model.Player;
import irden.space.proxy.plugin.player_manager.model.TempPlayer;
import irden.space.proxy.plugin.player_manager.persistence.PlayerJdbcRepository;
import irden.space.proxy.plugin.player_manager.persistence.model.PlayerRecord;
import irden.space.proxy.plugin.player_manager.roles.RoleManager;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketType;
import irden.space.proxy.protocol.payload.packet.client_connect.ClientConnect;
import irden.space.proxy.protocol.payload.packet.connect.ConnectFailure;
import irden.space.proxy.protocol.payload.packet.connect.ConnectSuccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;


@Component
public class PlayerConnectionHandler {

    private static final Logger log = LoggerFactory.getLogger(PlayerConnectionHandler.class);

    private final PlayerRegistry<Player> players;
    private final PlayerRegistry<TempPlayer> connectingPlayers;
    private final PlayerJdbcRepository playerRepository;
    private final PlayerAccessService playerAccessService;
    private final SessionPermissionService sessionPermissionService;
    private final RoleManager roleManager;
    private final ApplicationEventPublisher eventPublisher;

    public PlayerConnectionHandler(
            @Qualifier("onlinePlayerRegistry") PlayerRegistry<Player> players,
            @Qualifier("connectingPlayerRegistry") PlayerRegistry<TempPlayer> connectingPlayers,
            PlayerJdbcRepository playerRepository,
            PlayerAccessService playerAccessService,
            SessionPermissionService sessionPermissionService, RoleManager roleManager, ApplicationEventPublisher eventPublisher
    ) {
        this.players = players;
        this.connectingPlayers = connectingPlayers;
        this.playerRepository = playerRepository;
        this.playerAccessService = playerAccessService;
        this.sessionPermissionService = sessionPermissionService;
        this.roleManager = roleManager;
        this.eventPublisher = eventPublisher;
    }

    @PacketHandler(value = PacketType.CLIENT_CONNECT, direction = PacketDirection.TO_SERVER)
    @SuppressWarnings("unused")
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
    @SuppressWarnings("unused")
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
            playerAccessService.applyResolvedPermissions(
                    context.session().sessionId(),
                    tempPlayer.uuid().toString(),
                    tempPlayer.account()
            );
            roleManager.findRole(player.account()).ifPresent(s -> player.namePrefix(s.colorPrefix()));
            log.info(
                    "Player connected: name='{}', uuid={}, clientId={}, entityId={}",
                    player.name(),
                    player.uuid(),
                    player.clientId(),
                    player.entityId()
            );
            players.add(context.session().sessionId(), player);
            context.session().attributes().putIfAbsent("player", player);
            CompletableFuture.runAsync(() -> eventPublisher.publishEvent(new PlayerConnectedEvent(context.session().sessionId(), player)));
            return PacketDecision.forward();


        }
        ConnectFailure connectFailure = new ConnectFailure("Player connection state not found. Please try again.");
        context.session().sendToClient(PacketType.CONNECT_FAILURE, connectFailure);
        return PacketDecision.cancel();
    }

    @OnConnectionSuccess
    @SuppressWarnings("unused")
    public void onConnectionSuccess(PluginSessionContext context) {
        if (log.isInfoEnabled()) log.info("Session {} connected successfully.", context.sessionId());
    }

    @OnDisconnecting
    @SuppressWarnings("unused")
    public void onDisconnecting(PluginSessionContext context) {
        if (log.isInfoEnabled()) log.info("Session {} is disconnecting.", context.sessionId());
    }

    @OnDisconnected
    @SuppressWarnings("unused")
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
}
