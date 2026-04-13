package irden.space.proxy.plugin.debug;

import irden.space.proxy.plugin.api.*;
import irden.space.proxy.plugin.command_handler.CommandContext;
import irden.space.proxy.plugin.command_handler.ChatCommand;
import irden.space.proxy.plugin.debug.model.Player;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketType;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;
import irden.space.proxy.protocol.payload.packet.client_connect.ClientConnect;
import irden.space.proxy.protocol.payload.packet.connect.ConnectFailure;
import irden.space.proxy.protocol.payload.packet.connect.ConnectSuccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

@PluginDefinition(
        id = "debug-logger",
        name = "Debug Logger",
        version = "1.0.0",
        dependsOn = {"command-handler"},
        author = "https://github.com/Mofurka",
        description = "A plugin that logs all packets and lifecycle events for debugging purposes."
)
public class DebugLoggerPlugin implements ProxyPlugin {
    private static final Logger log = LoggerFactory.getLogger(DebugLoggerPlugin.class);
    private final Map<String, Player> tempPlayersMap = new LinkedHashMap<>();
    private final Map<StarUuid, Player> playersByUuid = new LinkedHashMap<>();

    @OnLoad
    public void handleLoad(PluginContext context) {
        log.info("Loading plugin '{}'", descriptor().id());
    }

    @OnStart
    public void handleStart() {
        log.info("Started plugin '{}'", descriptor().id());
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60000); // Log connected players every 60 seconds
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                if (!playersByUuid.isEmpty()) {
                    log.info("Currently connected players: {}", playersByUuid.values().stream().map(Player::name).toList());
                }
            }
        }).start();
    }

    @OnStop
    public void handleStop() {
        log.info("Stopped plugin '{}'", descriptor().id());
    }

    @PacketHandler(value = PacketType.PROTOCOL_REQUEST, direction = PacketDirection.TO_SERVER)
    public PacketDecision onProtocolRequest(PacketInterceptionContext context) {
        return logPacket("onProtocolRequest", context);
    }

    // This packet is sent by the client when it tries to connect to the server. We can use it to track player connections.
    @PacketHandler(value = PacketType.CLIENT_CONNECT, direction = PacketDirection.TO_SERVER)
    public PacketDecision onClientConnect(PacketInterceptionContext context) {
        ClientConnect clientConnect = (ClientConnect) context.parsedPayload();
        String s = context.session().sessionId();
        Player player = new Player(clientConnect.playerName(),
                clientConnect.playerUuid(),
                context.session().clientIp(),
                context.session().sessionId()
        );
        tempPlayersMap.put(s, player);
        return PacketDecision.forward();
    }

    // This packet is sent by the server when the client successfully connects. We can use it to confirm player connections and log additional info.
    @PacketHandler(value = PacketType.CONNECT_SUCCESS, direction = PacketDirection.TO_CLIENT)
    public PacketDecision onConnectSuccess(PacketInterceptionContext context) {
        ConnectSuccess connectSuccess = (ConnectSuccess) context.parsedPayload();
        Player player = tempPlayersMap.get(context.session().sessionId());
        if (player != null) {
            player.clientId(connectSuccess.clientId());
            player.entityId(connectSuccess.clientId() * -65536); // This how clientId transforms to entityId.
            playersByUuid.put(player.uuid(), player);
            log.info(
                    "Player connected: name='{}', uuid={}, clientId={}, entityId={}",
                    player.name(),
                    player.uuid(),
                    player.clientId(),
                    player.entityId()
            );
            tempPlayersMap.remove(context.session().sessionId());
            return PacketDecision.forward();
        }
        ConnectFailure connectFailure = new ConnectFailure(
                "Connection failed: Player not found in tracking map. This is likely a bug in the DebugLoggerPlugin."
        );
        context.session().sendToClient(PacketType.CONNECT_FAILURE, connectFailure);
        tempPlayersMap.remove(context.session().sessionId());
        return PacketDecision.cancel();
    }

    @PacketHandler(value = PacketType.PROTOCOL_RESPONSE, direction = PacketDirection.TO_CLIENT)
    public PacketDecision onProtocolResponse(PacketInterceptionContext context) {
        return logPacket("onProtocolResponse", context);
    }

    private PacketDecision logPacket(String handlerName, PacketInterceptionContext context) {
        log.info(
                "[PLUGIN][{}][{}] packetType={} parsed={}",
                handlerName,
                context.direction(),
                context.envelope().packetType(),
                context.parsedPayload()
        );
        return PacketDecision.forward();
    }

    @ChatCommand("debuglog")
    public void debugLogCommand(CommandContext context) {
        context.reply("DebugLoggerPlugin is active! Use this command to verify that the plugin is working.");
    }

    @ChatCommand("who")
    public void whoCommand(CommandContext context) {
        if (playersByUuid.isEmpty()) {
            context.reply("No players are currently connected.");
            return;
        }
        String s = "Connected players:\n";
        // example output:
        // Connected players:
        // Name1, Name2, Name3
        String join = String.join(",", playersByUuid.values().stream().map(Player::name).toList());
        context.reply(s + join);
    }
}
