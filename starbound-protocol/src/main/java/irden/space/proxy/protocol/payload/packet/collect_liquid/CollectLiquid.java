package irden.space.proxy.protocol.payload.packet.collect_liquid;

import irden.space.proxy.protocol.payload.common.vectors.StarVec2I;

import java.util.List;

public record CollectLiquid(
        List<StarVec2I> tilePositions,
        int liquidId
) {
}
