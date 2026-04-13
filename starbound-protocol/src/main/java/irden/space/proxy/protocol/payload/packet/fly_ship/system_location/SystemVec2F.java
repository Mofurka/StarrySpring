package irden.space.proxy.protocol.payload.packet.fly_ship.system_location;

import irden.space.proxy.protocol.payload.common.vectors.StarVec2F;

public record SystemVec2F(
        StarVec2F coordinates
) implements SystemLocation {
}
