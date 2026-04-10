package irden.space.proxy.protocol.payload.packet.entity_message;

public record EntityIdTarget(
        int entityId
) implements EntityMessageTarget {
}

