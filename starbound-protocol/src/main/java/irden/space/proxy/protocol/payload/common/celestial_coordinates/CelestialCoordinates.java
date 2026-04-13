package irden.space.proxy.protocol.payload.common.celestial_coordinates;

import irden.space.proxy.protocol.payload.common.vectors.StarVec3I;

public record CelestialCoordinates(
        StarVec3I location,
        int worldPlanetId,
        int worldSatelliteId
) {
}
