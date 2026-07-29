package irden.space.proxy.plugin.player_manager;

import irden.space.proxy.plugin.api.PacketDecision;
import irden.space.proxy.plugin.api.PacketInterceptionContext;
import irden.space.proxy.plugin.api.PluginSessionContext;
import irden.space.proxy.plugin.api.annotations.OnDisconnected;
import irden.space.proxy.plugin.api.annotations.PacketHandler;
import irden.space.proxy.plugin.player_manager.api.PlayerManagerApi;
import irden.space.proxy.plugin.player_manager.model.Player;
import irden.space.proxy.plugin.player_manager.model.player_position.*;
import irden.space.proxy.protocol.codec.variant.IntVariantValue;
import irden.space.proxy.protocol.codec.variant.MapVariantValue;
import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketType;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2F;
import irden.space.proxy.protocol.payload.common.vectors.StarVec3I;
import irden.space.proxy.protocol.payload.common.warp.action.ToAliasWarpAction;
import irden.space.proxy.protocol.payload.common.warp.action.ToAliasWarpAction.WarpAliasType;
import irden.space.proxy.protocol.payload.common.warp.action.ToPlayerWarpAction;
import irden.space.proxy.protocol.payload.common.warp.action.ToWorldWarpAction;
import irden.space.proxy.protocol.payload.common.warp.action.WarpAction;
import irden.space.proxy.protocol.payload.common.warp.target.*;
import irden.space.proxy.protocol.payload.packet.entity.type.player.PlayerNetState;
import irden.space.proxy.protocol.payload.packet.warp.player_warp_result.PlayerWarpResult;
import irden.space.proxy.protocol.payload.packet.world_start.WorldStart;
import irden.space.proxy.protocol.util.MapVariantUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


@Component
@Slf4j
@RequiredArgsConstructor
public class PlayerPositionTracker {
    private static final Map<String, PlayerPosition> playerPositionMap = new ConcurrentHashMap<>();
    private final PlayerManagerApi playerManagerApi;

    @PacketHandler(value = PacketType.PLAYER_WARP_RESULT, direction = PacketDirection.TO_CLIENT)
    public PacketDecision onPlayerWarpResult(PacketInterceptionContext ctx) {
        PlayerWarpResult result = ctx.parsedPayload(PlayerWarpResult.class);
        if (result == null || !result.warpSuccess()) {
            return PacketDecision.forward();
        }

        Optional<Player> maybePlayer = playerManagerApi.getPlayerBySessionId(ctx.session().sessionId());
        if (maybePlayer.isEmpty()) {
            return PacketDecision.forward();
        }
        Player player = maybePlayer.get();
        PlayerPosition position = positionOf(ctx.session().sessionId(), player);

        applyWarp(position, player, result.warpAction());
        return PacketDecision.forward();
    }

    // Определяем новую локацию по типу варпа. Для RETURN просто меняем местами current/previous.
    // Для ORBITED и CELESTIAL_WORLD координаты придут из world_start - здесь оставляем current как есть,
    // сдвигая только previous (world_start перезапишет current на CelestialWorld).
    private void applyWarp(PlayerPosition position, Player player, WarpAction warpAction) {
        PlayerLocation current = position.getCurrentLocation();

        // RETURN - возврат в предыдущую локацию: swap.
        if (warpAction instanceof ToAliasWarpAction(WarpAliasType aliasId) && aliasId == WarpAliasType.RETURN) {
            position.setCurrentLocation(position.getPreviousLocation());
            position.setPreviousLocation(current);
            return;
        }

        PlayerLocation next = resolveLocation(player, warpAction);
        position.setPreviousLocation(current);
        if (next != null) {
            position.setCurrentLocation(next);
        }
    }

    // Возвращает новую локацию, либо null если её должен доопределить world_start (celestial-миры без координат в варпе).
    private PlayerLocation resolveLocation(Player player, WarpAction warpAction) {
        return switch (warpAction) {
            case ToAliasWarpAction(WarpAliasType aliasId) -> switch (aliasId) {
                case SHIP -> new PlayerShip(player.uuid());
                case ORBITED -> null; // спуск на орбитируемую планету - координаты из world_start
                case RETURN -> null;  // обработано в applyWarp
            };
            case ToPlayerWarpAction(var playerIdHex) ->
                    playerManagerApi.findPlayerByUuid(playerIdHex.toHex(), true)
                            .map(target -> target.position().getCurrentLocation())
                            .orElse(null);
            case ToWorldWarpAction(WorldTarget target) -> switch (target) {
                case CelestialWorldTarget(var coordinates, _) ->
                        new CelestialWorld(coordinates.location(), coordinates.worldPlanetId(), coordinates.worldSatelliteId());
                case PlayerWorldTarget(var shipUuid, _) -> new PlayerShip(shipUuid);
                case UniqueWorldTarget(var worldName, var instanceUuid, _, _) ->
                        new InstanceWorld(worldName, instanceUuid);
                case MissionWorldTarget(var worldName) -> new InstanceWorld(worldName, null);
            };
        };
    }

    // Если при старте мира отсутствуют celestialParameters, то это скорее всего какой-нибудь инстанс или корабль игрока
    // celestialParameters есть только у celestial world, у всех остальных его нет, ибо это инстансы не расположенные в системе
    @PacketHandler(value = PacketType.WORLD_START, direction = PacketDirection.TO_CLIENT)
    public PacketDecision onWorldStart(PacketInterceptionContext ctx) {
        WorldStart worldStart = ctx.parsedPayload(WorldStart.class);
        Optional<Player> maybePlayer = playerManagerApi.getPlayerBySessionId(ctx.session().sessionId());
        if (maybePlayer.isEmpty()) {
            return PacketDecision.cancel();
        }

        Player player = maybePlayer.get();
        PlayerPosition position = positionOf(ctx.session().sessionId(), player);

        StarVec2F playerStart = worldStart.playerStart();
        position.setX(playerStart.x());
        position.setY(playerStart.y());

        readCelestialWorld(worldStart.templateData()).ifPresent(position::setCurrentLocation);

        return PacketDecision.forward();
    }

    private Optional<CelestialWorld> readCelestialWorld(VariantValue templateData) {
        if (!(templateData instanceof MapVariantValue template)) {
            return Optional.empty();
        }

        VariantValue[] location = MapVariantUtils.getList(template, "celestialParameters", "coordinate", "location");
        Integer planet = MapVariantUtils.getInt(template, "celestialParameters", "coordinate", "planet");
        Integer satellite = MapVariantUtils.getInt(template, "celestialParameters", "coordinate", "satellite");

        if (location == null || location.length != 3 || planet == null || satellite == null) {
            return Optional.empty();
        }
        if (!(location[0] instanceof IntVariantValue(int x)
                && location[1] instanceof IntVariantValue(int y)
                && location[2] instanceof IntVariantValue(int z))) {
            return Optional.empty();
        }

        return Optional.of(new CelestialWorld(new StarVec3I(x, y, z), planet, satellite));
    }

    @PacketHandler(value = PacketType.ENTITY_UPDATE, direction = PacketDirection.TO_SERVER)
    public PacketDecision onEntityUpdate(PacketInterceptionContext ctx) {
        PlayerNetState playerNetState = ctx.parsedPayload(PlayerNetState.class);
        if (playerNetState == null || playerNetState.movementController() == null) {
            return PacketDecision.forward();
        }

        PlayerPosition position = playerPositionMap.get(ctx.session().sessionId());
        if (position == null) {
            return PacketDecision.forward();
        }

        if (playerNetState.movementController().xPosition() != null) {
            position.setX(playerNetState.movementController().xPosition());
        }
        if (playerNetState.movementController().yPosition() != null) {
            position.setY(playerNetState.movementController().yPosition());
        }

        return PacketDecision.forward();
    }

    @OnDisconnected
    public void onDisconnectingSession(PluginSessionContext session) {
        playerPositionMap.remove(session.sessionId());
    }

    private PlayerPosition positionOf(String sessionId, Player player) {
        return playerPositionMap.computeIfAbsent(sessionId, k -> player.position());
    }
}
