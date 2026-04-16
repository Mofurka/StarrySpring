package irden.space.proxy.plugin.debug;

import irden.space.proxy.plugin.api.*;
import irden.space.proxy.plugin.command_handler.ChatCommand;
import irden.space.proxy.plugin.command_handler.CommandContext;
import irden.space.proxy.plugin.debug.model.Player;
import irden.space.proxy.protocol.assets.pak.GameAssetStores;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketType;
import irden.space.proxy.protocol.payload.common.damage.DamageRequest;
import irden.space.proxy.protocol.payload.common.damage.consts.DamageType;
import irden.space.proxy.protocol.payload.common.damage.consts.HitType;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2F;
import irden.space.proxy.protocol.payload.packet.client_connect.ClientConnect;
import irden.space.proxy.protocol.payload.packet.connect.ConnectFailure;
import irden.space.proxy.protocol.payload.packet.connect.ConnectSuccess;
import irden.space.proxy.protocol.payload.packet.damage.RemoteDamageRequest;
import irden.space.proxy.protocol.payload.packet.entity_create.EntityCreate;
import irden.space.proxy.protocol.payload.packet.entity_create.PlayerEntity;
import irden.space.proxy.protocol.payload.packet.entity_create.player.HumanoidIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
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
        }, "Online Checker").start();
        byte[] bytes = GameAssetStores.defaultStore().readAsset("/monsters/nightcolors.config");
        new String(bytes).lines().forEach(line -> log.info("/monsters/nightcolors.config line: {}", line));
    }

    @OnStop
    public void handleStop() {
        log.info("Stopped plugin '{}'", descriptor().id());
    }

    @PacketHandler(value = PacketType.PROTOCOL_REQUEST, direction = PacketDirection.TO_SERVER)
    public PacketDecision onProtocolRequest(PacketInterceptionContext context) {
        return logPacket("onProtocolRequest", context);
    }

//    @PacketHandler(value = PacketType.CELESTIAL_REQUEST)
//    public PacketDecision onCelestialRequest(PacketInterceptionContext context) {
//        return logPacket("onCelestialRequest", context);
//    }

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
            return PacketDecision.forward();
        }
        ConnectFailure connectFailure = new ConnectFailure(
                "Connection failed: Player not found in tracking map. This is likely a bug in the DebugLoggerPlugin."
        );
        context.session().sendToClient(PacketType.CONNECT_FAILURE, connectFailure);
        return PacketDecision.cancel();
    }

    @PacketHandler(value = PacketType.PROTOCOL_RESPONSE, direction = PacketDirection.TO_CLIENT)
    public PacketDecision onProtocolResponse(PacketInterceptionContext context) {
        return logPacket("onProtocolResponse", context);
    }

    @PacketHandler(value = PacketType.ENTITY_CREATE, direction = PacketDirection.TO_CLIENT)
    //test
    public PacketDecision onEntityCreate(PacketInterceptionContext context) {
        EntityCreate entityCreate = (EntityCreate) context.parsedPayload();
        if (entityCreate instanceof PlayerEntity player) {
            StringBuilder sb = new StringBuilder();
            sb.append("Session ID: ").append(context.session().sessionId()).append(System.lineSeparator());
            sb.append("Direction: ").append(context.direction()).append(System.lineSeparator());
            sb.append(player.uuid()).append(System.lineSeparator());
            sb.append(player.description()).append(System.lineSeparator());
            sb.append(player.modeType()).append(System.lineSeparator());
            HumanoidIdentity hi = player.humanoidIdentity();
            sb.append(hi.name()).append(System.lineSeparator());
            sb.append(hi.species()).append(System.lineSeparator());
            log.info("PlayerEntity created with the following details:\n{}", sb);


            new PlayerEntity(
                    player.uuid(),
                    player.description(),
                    player.modeType(),
                    new HumanoidIdentity(
                            "Неизвестный",
                            hi.species(),
                            hi.gender(),
                            hi.hairGroup(),
                            hi.hairType(),
                            hi.hairDirectives(),
                            hi.bodyDirectives(),
                            hi.emoteDirectives(),
                            hi.facialHairGroup(),
                            hi.facialHairType(),
                            hi.facialHairDirectives(),
                            hi.facialMaskGroup(),
                            hi.facialMaskType(),
                            hi.facialMaskDirectives(),
                            hi.personality(),
                            hi.color(),
                            hi.imagePath()
                    ),
                    player.firstNetState(),
                    player.entityId()
            );
            context.session().sendToClient(PacketType.ENTITY_CREATE, player);
            return PacketDecision.cancel(); // Cancel the original packet since we've sent a modified one.


        }



        return PacketDecision.forward();
    }

    @PacketHandler(value = PacketType.MODIFY_TILE_LIST)
    public PacketDecision onModifyTileList(PacketInterceptionContext context) {
        return logPacket("onModifyTileList", context);
    }

    @PacketHandler(value = PacketType.ENTITY_INTERACT)
    public PacketDecision onEntityInteract(PacketInterceptionContext context) {
        return logPacket("onEntityInteract", context);
    }

    @PacketHandler(value = PacketType.ENTITY_INTERACT_RESULT)
    public PacketDecision onEntityInteractResult(PacketInterceptionContext context) {
        return logPacket("onEntityInteractResult", context);
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

    @ChatCommand("debuglog") // checked
    public void debugLogCommand(CommandContext context) {
        context.reply("DebugLoggerPlugin is active! Use this command to verify that the plugin is working.");
    }
    @ChatCommand("kill") // checked
    public void killCommand(CommandContext context) {
        Player player = tempPlayersMap.get(context.session().sessionId());
        int i = player.entityId();
        DamageRequest damageRequest = new DamageRequest(
                HitType.KILL,
                DamageType.IGNORES_DEF,
                Float.MAX_VALUE,
                new StarVec2F(0, 0),
                0,
                "firebroadsword",
                Collections.emptyList()
        );
        RemoteDamageRequest remoteDamageRequest = new RemoteDamageRequest(
                i,
                i,
                damageRequest
        );
        context.session().sendToClient(PacketType.REMOTE_DAMAGE_REQUEST, remoteDamageRequest);
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
