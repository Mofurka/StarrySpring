package irden.space.proxy.plugin.planet_protect;

import irden.space.proxy.plugin.api.PacketInterceptionContext;
import irden.space.proxy.plugin.player_manager.api.PlayerManagerApi;
import irden.space.proxy.plugin.player_manager.model.Player;
import irden.space.proxy.plugin.player_manager.model.player_position.CelestialWorld;
import irden.space.proxy.plugin.player_manager.model.player_position.InstanceWorld;
import irden.space.proxy.plugin.player_manager.model.player_position.PlayerLocation;
import irden.space.proxy.plugin.player_manager.model.player_position.PlayerShip;
import irden.space.proxy.protocol.codec.variant.DoubleVariantValue;
import irden.space.proxy.protocol.codec.variant.IntVariantValue;
import irden.space.proxy.protocol.codec.variant.ListVariantValue;
import irden.space.proxy.protocol.codec.variant.MapVariantValue;
import irden.space.proxy.protocol.codec.variant.StringVariantValue;
import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.payload.common.star_pair.StarPair;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2I;
import irden.space.proxy.protocol.payload.packet.damage_tile_group.DamageTileGroup;
import irden.space.proxy.protocol.payload.packet.modify_tile_list.ModifyTileList;
import irden.space.proxy.protocol.payload.packet.modify_tile_list.tile_modification.PlaceLiquid;
import irden.space.proxy.protocol.payload.packet.modify_tile_list.tile_modification.PlaceMaterial;
import irden.space.proxy.protocol.payload.packet.modify_tile_list.tile_modification.PlaceMaterialColor;
import irden.space.proxy.protocol.payload.packet.modify_tile_list.tile_modification.PlaceMod;
import irden.space.proxy.protocol.payload.packet.modify_tile_list.tile_modification.TileModification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlanetModificationAuditLog {
    private static final int MAX_ENTRIES_PER_LINE = 256;

    private static final String UNKNOWN = "unknown";

    private final PlayerManagerApi playerManagerApi;

    public void tilesModified(PacketInterceptionContext context, ModifyTileList payload) {
        if (!log.isInfoEnabled()) {
            return;
        }

        List<StarPair<StarVec2I, TileModification>> modifications = payload.modificationList() == null
                ? List.of()
                : payload.modificationList().modifications();

        StringBuilder line = new StringBuilder(128);
        line.append("MODIFY_TILE ");
        appendActor(line, context);
        line.append(" overlap=").append(payload.allowEntityOverlap());
        line.append(" count=").append(modifications.size());

        int printed = 0;
        for (StarPair<StarVec2I, TileModification> modification : modifications) {
            if (printed++ == MAX_ENTRIES_PER_LINE) {
                line.append(" ...+").append(modifications.size() - MAX_ENTRIES_PER_LINE).append(" more");
                break;
            }
            appendModification(line, modification.first(), modification.second());
        }

        log.info("{}", line);
    }

    public void tileGroupDamaged(PacketInterceptionContext context, DamageTileGroup payload) {
        if (!log.isInfoEnabled()) {
            return;
        }

        List<StarVec2I> positions = payload.tilePositions() == null ? List.of() : payload.tilePositions();

        StringBuilder line = new StringBuilder(128);
        line.append("DAMAGE_TILE ");
        appendActor(line, context);
        line.append(" layer=").append(payload.layer());

        if (payload.tileDamage() != null) {
            line.append(" damage=").append(payload.tileDamage().type())
                    .append(':').append(payload.tileDamage().amount())
                    .append(" harvest=").append(payload.tileDamage().harvestLevel());
        }

        if (payload.sourcePosition() != null) {
            line.append(" from=").append(payload.sourcePosition().x())
                    .append(',').append(payload.sourcePosition().y());
        }

        payload.sourceEntity().ifPresent(entity -> line.append(" sourceEntity=").append(entity));
        line.append(" count=").append(positions.size());

        int printed = 0;
        for (StarVec2I position : positions) {
            if (printed++ == MAX_ENTRIES_PER_LINE) {
                line.append(" ...+").append(positions.size() - MAX_ENTRIES_PER_LINE).append(" more");
                break;
            }
            line.append(' ').append(position.x()).append(',').append(position.y());
        }

        log.info("{}", line);
    }

    public void objectPlaced(PacketInterceptionContext context, String objectName, VariantValue parameters) {
        if (!log.isInfoEnabled()) {
            return;
        }

        StringBuilder line = new StringBuilder(128);
        line.append("PLACE_OBJECT ");
        appendActor(line, context);
        line.append(" object=").append(objectName);
        line.append(" at=").append(objectPosition(parameters));

        String direction = stringParameter(parameters, "direction");
        if (direction != null) {
            line.append(" direction=").append(direction);
        }

        log.info("{}", line);
    }

    private void appendActor(StringBuilder line, PacketInterceptionContext context) {
        String sessionId = context.session() == null ? UNKNOWN : context.session().sessionId();
        Optional<Player> player = sessionId == null
                ? Optional.empty()
                : playerManagerApi.findPlayerBySessionId(sessionId);

        if (player.isPresent()) {
            Player found = player.get();
            line.append("player=").append(found.name())
                    .append(" uuid=").append(found.uuid())
                    .append(" account=").append(found.account())
                    .append(" world=").append(formatWorld(found.position().getCurrentLocation()));
            return;
        }

        line.append("player=").append(UNKNOWN)
                .append(" session=").append(sessionId)
                .append(" ip=").append(context.session() == null ? UNKNOWN : context.session().clientIp())
                .append(" world=").append(UNKNOWN);
    }

    private static String formatWorld(PlayerLocation location) {
        return switch (location) {
            case null -> UNKNOWN;
            case CelestialWorld(var at, var planet, var satellite) ->
                    "CelestialWorld:%d:%d:%d:%d:%d".formatted(at.x(), at.y(), at.z(), planet, satellite);
            case InstanceWorld(var worldName, var instanceUuid) ->
                    "InstanceWorld:%s:%s".formatted(worldName, instanceUuid);
            case PlayerShip(var shipUuid) -> "ClientShipWorld:%s".formatted(shipUuid);
        };
    }

    private static void appendModification(StringBuilder line, StarVec2I position, TileModification modification) {
        line.append(' ');
        if (position == null) {
            line.append("?,?");
        } else {
            line.append(position.x()).append(',').append(position.y());
        }
        line.append('=');

        switch (modification) {
            case null -> line.append(UNKNOWN);
            case PlaceMaterial(var layer, var materialId, var materialHue, var collisionOverride) -> {
                line.append("material:").append(layer).append(":id=").append(materialId);
                materialHue.ifPresent(hue -> line.append(":hue=").append(hue));
                if (collisionOverride != null
                        && collisionOverride != PlaceMaterial.TileCollisionOverride.NONE) {
                    line.append(":collision=").append(collisionOverride);
                }
            }
            case PlaceMod(var layer, var mod, var modHueShift) -> {
                line.append("mod:").append(layer).append(":id=").append(mod);
                modHueShift.ifPresent(hue -> line.append(":hue=").append(hue));
            }
            case PlaceMaterialColor(var layer, var color) ->
                    line.append("color:").append(layer).append(":value=").append(color);
            case PlaceLiquid(var liquidId, var liquidLevel) ->
                    line.append("liquid:id=").append(liquidId).append(":level=").append(liquidLevel);
        }
    }

    private static String objectPosition(VariantValue parameters) {
        VariantValue tilePosition = parameter(parameters, "tilePosition");
        if (tilePosition instanceof ListVariantValue(VariantValue[] coordinates) && coordinates.length >= 2) {
            return number(coordinates[0]) + "," + number(coordinates[1]);
        }
        return UNKNOWN;
    }

    private static String stringParameter(VariantValue parameters, String key) {
        return parameter(parameters, key) instanceof StringVariantValue(String value) ? value : null;
    }

    private static VariantValue parameter(VariantValue parameters, String key) {
        if (parameters instanceof MapVariantValue(Map<String, VariantValue> values) && values != null) {
            return values.get(key);
        }
        return null;
    }

    private static String number(VariantValue value) {
        return switch (value) {
            case IntVariantValue(long number) -> Long.toString(number);
            case DoubleVariantValue(double number) -> Long.toString(Math.round(number));
            case null, default -> "?";
        };
    }
}
