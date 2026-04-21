package irden.space.proxy.persistence.jpa.mapper;

import irden.space.proxy.domain.player.Player;
import irden.space.proxy.domain.player.PlayerId;
import irden.space.proxy.persistence.jpa.entity.PlayerEntity;

public final class PlayerMapper {

    private PlayerMapper() {
    }

    public static Player toDomain(PlayerEntity entity) {
        return new Player(
                new PlayerId(entity.getId()),
                entity.getUsername(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static PlayerEntity toEntity(Player domain) {
        return new PlayerEntity(
                domain.getId().value(),
                domain.getUsername(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }
}