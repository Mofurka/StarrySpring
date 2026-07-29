package irden.space.proxy.plugin.player_manager.model.player_position;

import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;


public record InstanceWorld(
        String worldName,
        StarUuid instanceUuid
) implements PlayerLocation {
}
