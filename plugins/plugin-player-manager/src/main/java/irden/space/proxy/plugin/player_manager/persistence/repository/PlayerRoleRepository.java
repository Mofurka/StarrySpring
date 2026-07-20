package irden.space.proxy.plugin.player_manager.persistence.repository;

import irden.space.proxy.plugin.player_manager.persistence.model.PlayerRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerRoleRepository extends JpaRepository<PlayerRoleEntity, UUID> {

    List<PlayerRoleEntity> findByPlayerUuidOrderByAssignedAtAscRoleNameAsc(String playerUuid);

    Optional<PlayerRoleEntity> findByPlayerUuidAndRoleName(String playerUuid, String roleName);

    @Transactional
    @Modifying
    @Query("DELETE FROM PlayerRoleEntity r WHERE r.playerUuid = :playerUuid AND r.roleName = :roleName")
    void removeRole(@Param("playerUuid") String playerUuid, @Param("roleName") String roleName);


    @Transactional
    default void assignRole(String playerUuid, String roleName, String assignedBy) {
        PlayerRoleEntity role = findByPlayerUuidAndRoleName(playerUuid, roleName)
                .orElseGet(PlayerRoleEntity::new);
        role.setPlayerUuid(playerUuid);
        role.setRoleName(roleName);
        role.setAssignedBy(assignedBy);
        role.setAssignedAt(LocalDateTime.now());
        save(role);
    }
}
