package irden.space.proxy.plugin.ban_manager.persistence.repository;

import irden.space.proxy.plugin.ban_manager.persistence.model.BanRecordEntity;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface BanRecordRepository extends JpaRepository<BanRecordEntity, UUID> {

    @Query("""
            SELECT b FROM BanRecordEntity b
            WHERE (b.playerUuid = :playerUuid OR b.ipAddress = :ipAddress OR b.name = :name)
              AND (b.permanent = TRUE OR b.expiresAt > CURRENT_TIMESTAMP)
            ORDER BY b.bannedAt DESC
            """)
    Optional<BanRecordEntity> findActiveBan(
            @Param("name") String name,
            @Param("playerUuid") String playerUuid,
            @Param("ipAddress") String ipAddress,
            Limit limit
    );

    @Transactional
    @Modifying
    @Query("""
            UPDATE BanRecordEntity b
            SET b.permanent = FALSE, b.expiresAt = CURRENT_TIMESTAMP
            WHERE b.ipAddress = :ipAddress
              AND (b.permanent = TRUE OR b.expiresAt > CURRENT_TIMESTAMP)
            """)
    int deactivateActiveBanByIpAddress(@Param("ipAddress") String ipAddress);

    @Transactional
    @Modifying
    @Query("""
            UPDATE BanRecordEntity b
            SET b.permanent = FALSE, b.expiresAt = CURRENT_TIMESTAMP
            WHERE (b.playerUuid = :playerUuid OR b.name = :name)
              AND (b.permanent = TRUE OR b.expiresAt > CURRENT_TIMESTAMP)
            """)
    int deactivateActiveBanByPlayer(@Param("playerUuid") String playerUuid, @Param("name") String name);
}
