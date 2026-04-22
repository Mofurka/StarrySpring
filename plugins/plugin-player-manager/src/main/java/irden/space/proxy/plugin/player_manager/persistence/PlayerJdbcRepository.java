package irden.space.proxy.plugin.player_manager.persistence;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    public Optional<PlayerRecord> findByUuid(UUID uuid) {
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
}