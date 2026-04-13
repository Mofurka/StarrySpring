package irden.space.proxy.protocol.payload.packet.fly_ship.system_location;

import irden.space.proxy.protocol.payload.common.celestial_coordinates.CelestialCoordinates;

public record CelestialSystemLocation(
        CelestialCoordinates coordinates
) implements SystemLocation {
}
