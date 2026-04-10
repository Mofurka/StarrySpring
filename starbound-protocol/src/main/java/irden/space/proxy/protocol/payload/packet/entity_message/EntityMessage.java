package irden.space.proxy.protocol.payload.packet.entity_message;

import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;

import java.util.List;

public record EntityMessage(
        EntityMessageTarget entityId,
        String message,
        List<VariantValue> args,
        StarUuid uuid,
        int fromConnection
) {
}
