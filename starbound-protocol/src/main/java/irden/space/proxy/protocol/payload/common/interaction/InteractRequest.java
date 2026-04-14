package irden.space.proxy.protocol.payload.common.interaction;

import irden.space.proxy.protocol.payload.common.vectors.StarVec2F;

public record InteractRequest(
        int entityId,
        StarVec2F position,
        int targetId,
        StarVec2F targetPosition
) {
}
