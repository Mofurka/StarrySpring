package irden.space.proxy.protocol.payload.packet.entity.type.player;

import irden.space.proxy.protocol.payload.common.vectors.StarVec2F;
import lombok.Builder;

@Builder
public record Personality(
        String idle,
        String armIdle,
        StarVec2F headOffset,
        StarVec2F armOffset
) {
}
