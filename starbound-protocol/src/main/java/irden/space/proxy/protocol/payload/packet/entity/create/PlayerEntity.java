package irden.space.proxy.protocol.payload.packet.entity.create;

import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;
import irden.space.proxy.protocol.payload.packet.entity.player.HumanoidIdentity;
import irden.space.proxy.protocol.payload.packet.entity.player.PlayerFirstNetState;
import lombok.Builder;

@Builder
public record PlayerEntity (
        StarUuid uuid,
        String description,
        int modeType,
        HumanoidIdentity humanoidIdentity,
        PlayerFirstNetState firstNetState,
        int entityId
) implements Entity {
}
