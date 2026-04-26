package irden.space.proxy.protocol.payload.common.warp.target;

import irden.space.proxy.protocol.payload.common.celestial_coordinates.CelestialCoordinates;
import irden.space.proxy.protocol.payload.packet.warp.consts.SpawnTarget;

public record CelestialWorldTarget(
        CelestialCoordinates celestialCoordinates,
        SpawnTarget spawnTarget
) implements WorldTarget {
}
