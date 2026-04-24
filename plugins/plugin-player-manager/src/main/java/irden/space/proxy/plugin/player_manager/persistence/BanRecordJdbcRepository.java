package irden.space.proxy.plugin.player_manager.persistence;

import irden.space.proxy.plugin.player_manager.persistence.model.BanRecord;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;

public class BanRecordJdbcRepository {

    private final JdbcTemplate jdbcTemplate;
    private final BanRecordRowMapper rowMapper = new BanRecordRowMapper();

    public BanRecordJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }


    public void save(BanRecord banRecord) {
        String sql = "INSERT INTO player_manager_ban_records (name, player_uuid, ip_address, reason, banned_by, permanent, banned_at, expires_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                banRecord.name(),
                banRecord.playerUuid(),
                banRecord.ipAddress(),
                banRecord.reason(),
                banRecord.bannedBy(),
                banRecord.permanent(),
                banRecord.bannedAt(),
                banRecord.expiresAt());
    }

    public Optional<BanRecord> findActiveBanByBanRecord(BanRecord banRecord) {
        String sql = "SELECT name, player_uuid, ip_address, reason, banned_by, permanent, banned_at, expires_at " +
                "FROM player_manager_ban_records " +
                "WHERE (player_uuid = ? OR ip_address = ? OR name = ?) " +
                "AND (permanent = TRUE OR expires_at > NOW()) " +
                "ORDER BY banned_at DESC LIMIT 1";
        List<BanRecord> results = jdbcTemplate.query(sql,
                rowMapper,
                banRecord.playerUuid(),
                banRecord.ipAddress(),
                banRecord.name()
        );
        return results.stream().findFirst();
    }

    public int deactivateActiveBanByIpAddress(String ipAddress) {
        String sql = "UPDATE player_manager_ban_records " +
                "SET permanent = FALSE, expires_at = CURRENT_TIMESTAMP " +
                "WHERE ip_address = ? " +
                "AND (permanent = TRUE OR expires_at > CURRENT_TIMESTAMP)";
        return jdbcTemplate.update(sql, ipAddress);
    }

    public int deactivateActiveBanByPlayer(String playerUuid, String name) {
        String sql = "UPDATE player_manager_ban_records " +
                "SET permanent = FALSE, expires_at = CURRENT_TIMESTAMP " +
                "WHERE (player_uuid = ? OR name = ?) " +
                "AND (permanent = TRUE OR expires_at > CURRENT_TIMESTAMP)";
        return jdbcTemplate.update(sql, playerUuid, name);
    }

}