package irden.space.proxy.protocol.payload.packet.entity_interact;

import irden.space.proxy.protocol.payload.common.interaction.InteractRequest;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;

public record EntityInteract(
        InteractRequest interactRequest,
        StarUuid requestId
        ) {


}
