package irden.space.proxy.protocol.payload.packet.celestial_reqeust;

import irden.space.proxy.protocol.payload.common.star_either.StarEither;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2I;
import irden.space.proxy.protocol.payload.common.vectors.StarVec3I;

import java.util.List;

public record CelestialRequest(
        List<StarEither<StarVec2I, StarVec3I>> celestialRequests
) {
}
