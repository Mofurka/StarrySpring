package irden.space.proxy.protocol.payload.packet.entity_message;

import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;

public record UniqueEntityIdTarget(
        StarUuid uniqueEntityId
) implements EntityMessageTarget {
}

