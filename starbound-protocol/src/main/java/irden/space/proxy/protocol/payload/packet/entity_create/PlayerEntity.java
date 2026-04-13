package irden.space.proxy.protocol.payload.packet.entity_create;

import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;

public record PlayerEntity (
        String uuid,
        String description,
        int modeType,
        HumanoidIdentity humanoidIdentity,
        byte[] firstNetState,
        int entityId
) implements EntityCreate {
}
