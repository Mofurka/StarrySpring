package irden.space.proxy.protocol.payload.packet.fly_ship;

import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.payload.common.vectors.StarVec3I;
import irden.space.proxy.protocol.payload.packet.fly_ship.system_location.SystemLocation;

public record FlyShip(
        StarVec3I system,
        SystemLocation location,
        VariantValue settings
) {
}
