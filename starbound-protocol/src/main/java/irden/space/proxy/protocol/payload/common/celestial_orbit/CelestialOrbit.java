package irden.space.proxy.protocol.payload.common.celestial_orbit;

import irden.space.proxy.protocol.payload.common.vectors.StarVec2F;

public record CelestialOrbit(
        int direction,
        double enterTime,
        StarVec2F enterPosition
) {
}
