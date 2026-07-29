package irden.space.proxy.plugin.player_manager.model.player_position;

import irden.space.proxy.protocol.payload.common.vectors.StarVec3I;

import java.util.Objects;

public record CelestialWorld(
        StarVec3I location,
        int planet,
        int satellite
) implements PlayerLocation {

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CelestialWorld(StarVec3I location1, int planet1, int satellite1))) return false;
        return planet == planet1 && satellite == satellite1 && location.equals(location1);
    }

    @Override
    public int hashCode() {
        return Objects.hash(location, planet, satellite);
    }
}
