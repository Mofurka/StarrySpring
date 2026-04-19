package irden.space.proxy.plugin.debug;

import irden.space.proxy.plugin.api.*;
import irden.space.proxy.plugin.command_handler.ChatCommand;
import irden.space.proxy.plugin.command_handler.CommandContext;
import irden.space.proxy.plugin.debug.model.Player;
import irden.space.proxy.protocol.codec.variant.StringVariantValue;
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
import irden.space.proxy.protocol.payload.packet.entity.type.Entity;
import irden.space.proxy.protocol.payload.packet.entity.type.PlayerEntity;
import irden.space.proxy.protocol.payload.packet.entity.update.EffectsAnimator;
import irden.space.proxy.protocol.payload.packet.entity.type.player.PlayerNetState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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
                context.session().sessionId(),
                context.session()
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

    @PacketHandler(value = PacketType.ENTITY_CREATE, direction = PacketDirection.TO_CLIENT)
    public PacketDecision onEntityCreate(PacketInterceptionContext context) {
        Entity entity = (Entity) context.parsedPayload();

        if (entity instanceof PlayerEntity playerEntity) {
            Player player = tempPlayersMap.get(context.session().sessionId());
            if (playerEntity.entityId() != player.entityId() && player.entityId() != 0) {
                CompletableFuture.runAsync(() -> sendEntityNames(context.session(), playerEntity.entityId()));
                return PacketDecision.forward();
            }
        }
        return PacketDecision.forward();
    }

    private void sendEntityNames(PluginSessionContext session, int entityId) {
        var player = PlayerNetState.builder();
        var effectsAnimator = EffectsAnimator.builder();
        int connectionId = entityId / -65536;
        player.connectionId(connectionId);
        player.entityId(entityId);
        effectsAnimator.globalTags(Map.of("nametag", new StringVariantValue("Неизвестный ^cyan;[%s]^reset;".formatted(connectionId))));
        player.effectsAnimator(effectsAnimator.build());
        PlayerNetState build = player.build();
        for (int i = 0; i < 3; i++) {
            session.sendToClient(PacketType.ENTITY_UPDATE, build);
        }
    }

    @ChatCommand(value = "setnametag",
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
        effectsAnimator.globalTags(Map.of("nametag", new StringVariantValue(nametag + " ^cyan;[%s]^reset;".formatted(connectionIdStr))));
        player.effectsAnimator(effectsAnimator.build());
        player.entityId(entityId);
        player.connectionId(Integer.parseInt(connectionIdStr));
        context.session().sendToClient(PacketType.ENTITY_UPDATE, player.build());
        context.reply("Nametag update sent to connectionId=" + connectionIdStr);
    }



    @PacketHandler(value = PacketType.SPAWN_ENTITY)
    public PacketDecision onSpawnEntity(PacketInterceptionContext context) {
        return logPacket("onSpawnEntity", context);
    }

    @ChatCommand("incognito")
    public void setupIncognito(CommandContext context) {
        // Для начала просто отправка пакета персонажу
        var player = PlayerNetState.builder();
        var effectsAnimator = EffectsAnimator.builder();
        effectsAnimator.globalTags(Map.of("nametag", new StringVariantValue("Incognito")));
        player.effectsAnimator(effectsAnimator.build());
        //test
        playersByUuid.forEach((s, p) -> {
            if (p.name().equals("Misty")) {
                player.entityId(p.entityId());
                player.connectionId(p.clientId());
            }
        });
        playersByUuid.forEach((s, p) -> {
            if (p.name().equals("D.")) {
                p.sessionContext().sendToClient(PacketType.ENTITY_UPDATE, player.build());
            }
        });
        // УРА РАБОТАЕТ!
        context.reply("Incognito setup sent to D. for Misty. Check if the nametag changed to 'Incognito'.");
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
