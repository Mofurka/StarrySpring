package irden.space.proxy.plugin.irden.integration.persistence.repository;

import irden.space.proxy.plugin.irden.integration.persistence.model.PlayerAttributesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerAttributesRepository extends JpaRepository<PlayerAttributesEntity, UUID> {

    Optional<PlayerAttributesEntity> findByPlayerUuid(String playerUuid);

    List<PlayerAttributesEntity> findAllByApplicationId(Long applicationId);
}
