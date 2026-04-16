package irden.space.proxy.protocol.payload.packet.entity_create;

import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;
import irden.space.proxy.protocol.payload.packet.entity_create.player.HumanoidIdentity;
import irden.space.proxy.protocol.payload.packet.entity_create.player.PlayerNetState;

public record PlayerEntity (
        StarUuid uuid,
        String description,
        int modeType,
        HumanoidIdentity humanoidIdentity,
        PlayerNetState firstNetState,
        int entityId
) implements EntityCreate {
}
