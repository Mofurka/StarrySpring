package irden.space.proxy.protocol.payload.packet.entity.type;

import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;
import irden.space.proxy.protocol.payload.packet.entity.type.player.HumanoidIdentity;
import irden.space.proxy.protocol.payload.packet.entity.type.player.PlayerNetState;
import lombok.Builder;

@Builder
public record PlayerEntity (
        StarUuid uuid,
        String description,
        int modeType,
        HumanoidIdentity humanoidIdentity,
        PlayerNetState firstNetState,
        Integer entityId
) implements Entity {
}
