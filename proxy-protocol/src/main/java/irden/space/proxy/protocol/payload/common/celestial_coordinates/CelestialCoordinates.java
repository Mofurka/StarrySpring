package irden.space.proxy.protocol.payload.common.celestial_coordinates;

public record CelestialCoordinates(
        int x,
        int y,
        int z,
        int worldPlanetId,
        int worldSatelliteId
) {
}
