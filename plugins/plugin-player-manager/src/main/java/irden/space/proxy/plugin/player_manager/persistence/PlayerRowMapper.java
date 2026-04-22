package irden.space.proxy.plugin.player_manager.persistence;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

public class PlayerRowMapper implements RowMapper<PlayerRecord> {

    @Override
    public PlayerRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new PlayerRecord(
                rs.getObject("id", UUID.class),
                rs.getString("player_uuid"),
                rs.getString("name"),
                rs.getString("ip_address"),
                rs.getObject("created_at", LocalDateTime.class)
        );
    }
}