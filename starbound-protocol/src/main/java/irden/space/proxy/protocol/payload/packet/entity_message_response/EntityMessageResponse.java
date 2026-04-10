package irden.space.proxy.protocol.payload.packet.entity_message_response;

import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;

public record EntityMessageResponse(
        EntityMessageRsponseValue response,
        StarUuid uuid
) {
}
