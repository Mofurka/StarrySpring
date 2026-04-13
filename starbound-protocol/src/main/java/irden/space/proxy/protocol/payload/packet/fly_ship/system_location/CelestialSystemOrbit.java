package irden.space.proxy.protocol.payload.packet.fly_ship.system_location;

import irden.space.proxy.protocol.payload.common.celestial_coordinates.CelestialCoordinates;
import irden.space.proxy.protocol.payload.common.celestial_orbit.CelestialOrbit;

public record CelestialSystemOrbit(
        CelestialCoordinates celestialCoordinates,
        CelestialOrbit celestialOrbit
) implements SystemLocation{
}
