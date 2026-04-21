package irden.space.proxy.plugin.player_manager.model;

import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;
import lombok.Builder;

@Builder
public record TempPlayer (
        String name,
        String sessionId,
        StarUuid uuid
){

}
