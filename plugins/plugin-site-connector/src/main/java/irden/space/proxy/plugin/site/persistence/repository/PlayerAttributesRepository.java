package irden.space.proxy.plugin.site.persistence.repository;

import irden.space.proxy.plugin.site.persistence.model.PlayerAttributesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PlayerAttributesRepository extends JpaRepository<PlayerAttributesEntity, UUID> {

    Optional<PlayerAttributesEntity> findByPlayerUuid(String playerUuid);
}
