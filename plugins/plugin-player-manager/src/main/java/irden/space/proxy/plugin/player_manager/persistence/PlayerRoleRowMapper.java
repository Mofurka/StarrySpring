package irden.space.proxy.plugin.player_manager.persistence;

import irden.space.proxy.plugin.player_manager.persistence.model.PlayerRoleRecord;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PlayerRoleRowMapper implements RowMapper<PlayerRoleRecord> {

    private static final RowMapper<PlayerRoleRecord> DELEGATE = DataClassRowMapper.newInstance(PlayerRoleRecord.class);

    @Override
    @SuppressWarnings("NullableProblems")
    public PlayerRoleRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        return DELEGATE.mapRow(rs, rowNum);
    }
}

