package irden.space.proxy.protocol.payload.packet.entity.type;

import irden.space.proxy.protocol.payload.common.star_item.StarItemDescriptor;
import irden.space.proxy.protocol.payload.common.timers.EpochTimer;
import irden.space.proxy.protocol.payload.common.timers.GameTimer;

public record ItemDropEntity (
        StarItemDescriptor itemDescriptor,
        Boolean eternal,
        EpochTimer dropAge,
        GameTimer intangibleTimer,
        Integer entityId
) implements Entity {
}
