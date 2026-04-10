package irden.space.proxy.protocol.payload.common.warp.target;

import irden.space.proxy.protocol.payload.common.celestial_coordinates.CelestialCoordinates;

public record CelestialWorldTarget(
        CelestialCoordinates celestialCoordinates,
        String teleporter
) implements WorldTarget {
}
