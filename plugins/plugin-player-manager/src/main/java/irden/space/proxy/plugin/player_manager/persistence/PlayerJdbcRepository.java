package irden.space.proxy.plugin.player_manager.persistence;

import irden.space.proxy.plugin.player_manager.persistence.model.PlayerRecord;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;

public class PlayerJdbcRepository {

    private final JdbcTemplate jdbcTemplate;
    private final PlayerRowMapper rowMapper = new PlayerRowMapper();

    public PlayerJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(PlayerRecord player) {
        jdbcTemplate.update("""
                INSERT INTO player_manager_players (
                    player_uuid,
                    name,
                    ip_address,
                    created_at
                ) VALUES (?, ?, ?, ?)
                """,
                player.playerUuid(),
                player.name(),
                player.ipAddress(),
                player.createdAt()
        );
    }

    public void updatePlayerIpAddress(String uuid, String newIpAddress) {
        jdbcTemplate.update("""
                UPDATE player_manager_players
                SET ip_address = ?
                WHERE player_uuid = ?
                """,
                newIpAddress,
                uuid
        );
    }


    public Optional<PlayerRecord> findByUuid(String uuid) {
        List<PlayerRecord> results = jdbcTemplate.query("""
                SELECT id, player_uuid, name, ip_address, created_at
                FROM player_manager_players
                WHERE player_uuid = ?
                """,
                rowMapper,
                uuid
        );
        return results.stream().findFirst();
    }

    public Optional<PlayerRecord> findById(String id) {
        List<PlayerRecord> results = jdbcTemplate.query("""
                SELECT id, player_uuid, name, ip_address, created_at
                FROM player_manager_players
                WHERE id = ?
                """,
                rowMapper,
                id
        );
        return results.stream().findFirst();
    }

    public List<PlayerRecord> findAll() {
        return jdbcTemplate.query("""
                SELECT id, player_uuid, name, ip_address, created_at
                FROM player_manager_players
                """,
                rowMapper
        );
    }

    public void deleteByUuid(String uuid) {
        jdbcTemplate.update("""
                DELETE FROM player_manager_players
                WHERE player_uuid = ?
                """,
                uuid
        );
    }

    public void deleteById(String id) {
        jdbcTemplate.update("""
                DELETE FROM player_manager_players
                WHERE id = ?
                """,
                id
        );
    }


}