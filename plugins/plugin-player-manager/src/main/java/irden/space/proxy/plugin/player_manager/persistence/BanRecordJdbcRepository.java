package irden.space.proxy.plugin.player_manager.persistence;

import irden.space.proxy.plugin.player_manager.persistence.model.BanRecord;
import irden.space.proxy.plugin.player_manager.persistence.model.PlayerRecord;
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




}