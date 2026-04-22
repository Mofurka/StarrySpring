package irden.space.proxy.plugin.player_manager.persistence;

import irden.space.proxy.plugin.player_manager.persistence.model.PlayerRecord;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PlayerRowMapper implements RowMapper<PlayerRecord> {

    private static final RowMapper<PlayerRecord> DELEGATE = DataClassRowMapper.newInstance(PlayerRecord.class);

    @Override
    public PlayerRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        return DELEGATE.mapRow(rs, rowNum);
    }
}