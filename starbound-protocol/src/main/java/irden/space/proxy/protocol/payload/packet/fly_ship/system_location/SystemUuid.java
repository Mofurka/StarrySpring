package irden.space.proxy.protocol.payload.packet.fly_ship.system_location;

import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;

public record SystemUuid (
        StarUuid systemUuid
) implements SystemLocation{
}
