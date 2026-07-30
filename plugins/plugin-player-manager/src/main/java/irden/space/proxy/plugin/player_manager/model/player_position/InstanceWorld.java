package irden.space.proxy.plugin.player_manager.model.player_position;

import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;

import java.util.Objects;


public record InstanceWorld(
        String worldName,
        StarUuid instanceUuid
) implements PlayerLocation {
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof InstanceWorld(String name, StarUuid uuid))) return false;
        return Objects.equals(worldName, name) && Objects.equals(instanceUuid, uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(worldName, instanceUuid);
    }
}
