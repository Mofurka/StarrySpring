package irden.space.proxy.plugin.player_manager.model.player_position;

import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;

import java.util.Objects;


public record PlayerShip(
        StarUuid shipUuid
) implements PlayerLocation {
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PlayerShip(StarUuid uuid))) return false;
        return Objects.equals(shipUuid(), uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(shipUuid());
    }
}
