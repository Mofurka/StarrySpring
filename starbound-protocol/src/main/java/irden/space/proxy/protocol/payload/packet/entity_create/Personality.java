package irden.space.proxy.protocol.payload.packet.entity_create;

import irden.space.proxy.protocol.payload.common.vectors.StarVec2F;

public record Personality(
        String idle,
        String armIdle,
        StarVec2F headOffset,
        StarVec2F armOffset
) {
}
