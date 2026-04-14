package irden.space.proxy.protocol.payload.packet.entity_interact;

import irden.space.proxy.protocol.payload.common.interaction.InteractAction;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;

public record EntityInteractResult
        (
                InteractAction interactAction,
                StarUuid requestId
        ) {
}
