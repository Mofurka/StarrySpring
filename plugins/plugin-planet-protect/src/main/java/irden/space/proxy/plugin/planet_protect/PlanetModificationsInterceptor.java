package irden.space.proxy.plugin.planet_protect;

import irden.space.proxy.plugin.api.PacketDecision;
import irden.space.proxy.plugin.api.PacketInterceptionContext;
import irden.space.proxy.plugin.api.annotations.PacketHandler;
import irden.space.proxy.plugin.planet_protect.permissions.PlanetPermissions;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketType;
import irden.space.proxy.protocol.payload.common.star_item.StarItemDescriptor;
import irden.space.proxy.protocol.payload.packet.entity.type.Entity;
import irden.space.proxy.protocol.payload.packet.entity.type.ObjectEntity;
import irden.space.proxy.protocol.util.VariantObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlanetModificationsInterceptor {
    private final VariantObjectMapper variantObjectMapper;


    @PacketHandler(value = PacketType.MODIFY_TILE_LIST, direction = PacketDirection.TO_SERVER)
    public PacketDecision onModifyTileList(PacketInterceptionContext context) {
        if (context.session().permissions().has(PlanetPermissions.MODIFY_LIMIT)) {
            return PacketDecision.forward();
        }
        return PacketDecision.cancel();
    }

    @PacketHandler(value = PacketType.DAMAGE_TILE_GROUP, direction = PacketDirection.TO_SERVER)
    public PacketDecision onDamageTileGroup(PacketInterceptionContext context) {
        if (context.session().permissions().has(PlanetPermissions.MODIFY_LIMIT)) {
            return PacketDecision.forward();
        }
        return PacketDecision.cancel();
    }

    @PacketHandler(value = PacketType.SPAWN_ENTITY, direction = PacketDirection.TO_SERVER)
    public PacketDecision onSpawnEntity(PacketInterceptionContext context) {
        var spawnedEntity = context.parsedPayload(Entity.class);
        if (spawnedEntity instanceof ObjectEntity(var name, var parameters)) {
            if (context.session().permissions().has(PlanetPermissions.MODIFY_LIMIT)) {
                return PacketDecision.forward();
            } else {
                var sid = new StarItemDescriptor(name, 1, parameters);
                return PacketDecision.cancel(
                        () -> context.session().sendToClient(PacketType.GIVE_ITEM, sid)
                );
            }
        }
        return PacketDecision.forward();
    }
}
