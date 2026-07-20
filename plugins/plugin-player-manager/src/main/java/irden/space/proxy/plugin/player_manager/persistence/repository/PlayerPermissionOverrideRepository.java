package irden.space.proxy.plugin.player_manager.persistence.repository;

import irden.space.proxy.plugin.player_manager.persistence.model.PlayerPermissionOverrideEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerPermissionOverrideRepository extends JpaRepository<PlayerPermissionOverrideEntity, UUID> {

    List<PlayerPermissionOverrideEntity> findByPlayerUuidOrderByChangedAtAscPermissionNameAsc(String playerUuid);

    Optional<PlayerPermissionOverrideEntity> findByPlayerUuidAndPermissionName(String playerUuid, String permissionName);

    @Transactional
    @Modifying
    @Query("DELETE FROM PlayerPermissionOverrideEntity o WHERE o.playerUuid = :playerUuid AND o.permissionName = :permissionName")
    void deletePermissionOverride(@Param("playerUuid") String playerUuid, @Param("permissionName") String permissionName);

    @Transactional
    default void savePermissionOverride(String playerUuid, String permissionName, boolean granted, String changedBy) {
        PlayerPermissionOverrideEntity override = findByPlayerUuidAndPermissionName(playerUuid, permissionName)
                .orElseGet(PlayerPermissionOverrideEntity::new);
        override.setPlayerUuid(playerUuid);
        override.setPermissionName(permissionName);
        override.setGranted(granted);
        override.setChangedBy(changedBy);
        override.setChangedAt(LocalDateTime.now());
        save(override);
    }
}
