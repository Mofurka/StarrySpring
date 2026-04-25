package irden.space.proxy.protocol.payload.packet.entity_message;

import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;
import lombok.Builder;

import java.util.List;

@Builder
public record EntityMessage(
        EntityMessageTarget entityId,
        String message,
        VariantValue[] args,
        StarUuid uuid,
        int fromConnection
) {
}
