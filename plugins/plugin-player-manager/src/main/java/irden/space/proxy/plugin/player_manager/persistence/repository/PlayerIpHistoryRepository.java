package irden.space.proxy.plugin.player_manager.persistence.repository;

import irden.space.proxy.plugin.player_manager.ip.PlayerIpLink;
import irden.space.proxy.plugin.player_manager.persistence.model.PlayerIpHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PlayerIpHistoryRepository extends JpaRepository<PlayerIpHistoryEntity, UUID> {


    @Transactional
    @Modifying
    @Query(value = """
            insert into player_manager.player_ip_history
                (id, player_uuid, ip_address, first_seen, last_seen, connection_count)
            values (gen_random_uuid(), :playerUuid, :ipAddress, :now, :now, 1)
            on conflict on constraint uk_player_manager_ip_history_uuid_ip
            do update set last_seen = :now,
                          connection_count = player_ip_history.connection_count + 1
            """, nativeQuery = true)
    void recordConnection(
            @Param("playerUuid") String playerUuid,
            @Param("ipAddress") String ipAddress,
            @Param("now") LocalDateTime now
    );

    List<PlayerIpHistoryEntity> findByPlayerUuidOrderByLastSeenDesc(String playerUuid);


    @Query("""
            select new irden.space.proxy.plugin.player_manager.ip.PlayerIpLink(
                other.playerUuid, other.ipAddress, other.firstSeen, other.lastSeen, other.connectionCount)
            from PlayerIpHistoryEntity other, PlayerIpHistoryEntity mine
            where mine.playerUuid = :playerUuid
              and other.ipAddress = mine.ipAddress
              and other.playerUuid <> :playerUuid
            order by other.lastSeen desc
            """)
    List<PlayerIpLink> findLinkedPlayers(@Param("playerUuid") String playerUuid);
}
