package irden.space.proxy.plugin.player_manager.persistence;

import irden.space.proxy.plugin.player_manager.persistence.model.PlayerPermissionOverrideRecord;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PlayerPermissionOverrideRowMapper implements RowMapper<PlayerPermissionOverrideRecord> {

    private static final RowMapper<PlayerPermissionOverrideRecord> DELEGATE = DataClassRowMapper.newInstance(PlayerPermissionOverrideRecord.class);

    @Override
    @SuppressWarnings("NullableProblems")
    public PlayerPermissionOverrideRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        return DELEGATE.mapRow(rs, rowNum);
    }
}

