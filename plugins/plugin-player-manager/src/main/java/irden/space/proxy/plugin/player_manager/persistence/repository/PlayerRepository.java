package irden.space.proxy.plugin.player_manager.persistence.repository;

import irden.space.proxy.plugin.player_manager.persistence.model.PlayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerRepository extends JpaRepository<PlayerEntity, UUID> {

    Optional<PlayerEntity> findFirstByName(String name);

    Optional<PlayerEntity> findByPlayerUuid(String playerUuid);

    List<PlayerEntity> findByIpAddress(String ipAddress);

    @Transactional
    @Modifying
    @Query("UPDATE PlayerEntity p SET p.ipAddress = :ipAddress WHERE p.playerUuid = :playerUuid")
    void updateIpAddress(@Param("playerUuid") String playerUuid, @Param("ipAddress") String ipAddress);
}
