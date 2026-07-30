package irden.space.proxy.protocol.payload.packet.entity_message;

public record UniqueEntityIdTarget(
        String uniqueEntityId
) implements EntityMessageTarget {
}

