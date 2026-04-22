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
                insert into player_manager_players (
                    id,
                    player_uuid,
                    name,
                    ip_address,
                    created_at
                ) values (?, ?, ?, ?, ?)
                on conflict (id) do update set
                    player_uuid = excluded.player_uuid,
                    name = excluded.name,
                    ip_address = excluded.ip_address
                """,
                player.id(),
                player.playerUuid(),
                player.name(),
                player.ipAddress(),
                player.createdAt()
        );
    }

    public void updatePlayerIpAddress(String uuid, String newIpAddress) {
        jdbcTemplate.update("""
                update player_manager_players
                set ip_address = ?
                where player_uuid = ?
                """,
                newIpAddress,
                uuid
        );
    }


    public Optional<PlayerRecord> findByUuid(String uuid) {
        List<PlayerRecord> results = jdbcTemplate.query("""
                select id, player_uuid, name, ip_address, created_at
                from player_manager_players
                where player_uuid = ?
                """,
                rowMapper,
                uuid
        );
        return results.stream().findFirst();
    }

    public Optional<PlayerRecord> findById(String id) {
        List<PlayerRecord> results = jdbcTemplate.query("""
                select id, player_uuid, name, ip_address, created_at
                from player_manager_players
                where id = ?
                """,
                rowMapper,
                id
        );
        return results.stream().findFirst();
    }

    public List<PlayerRecord> findAll() {
        return jdbcTemplate.query("""
                select id, player_uuid, name, ip_address, created_at
                from player_manager_players
                """,
                rowMapper
        );
    }

    public void deleteByUuid(String uuid) {
        jdbcTemplate.update("""
                delete from player_manager_players
                where player_uuid = ?
                """,
                uuid
        );
    }

    public void deleteById(String id) {
        jdbcTemplate.update("""
                delete from player_manager_players
                where id = ?
                """,
                id
        );
    }


}