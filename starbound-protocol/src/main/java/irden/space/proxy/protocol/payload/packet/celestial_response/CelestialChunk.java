package irden.space.proxy.protocol.payload.packet.celestial_response;

import irden.space.proxy.protocol.payload.common.star_pair.StarPair;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2I;

import java.util.List;

public record CelestialChunk(
        StarVec2I chunkIndex,
        List<StarPair<StarVec2I, StarVec2I>> constellations
        // Thats a lot, i dont want to parse it T_T
        // Maybe soon

) {
}
