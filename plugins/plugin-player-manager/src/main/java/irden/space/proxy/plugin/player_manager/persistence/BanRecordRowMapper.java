package irden.space.proxy.plugin.player_manager.persistence;

import irden.space.proxy.plugin.player_manager.persistence.model.BanRecord;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class BanRecordRowMapper implements RowMapper<BanRecord> {

    private static final RowMapper<BanRecord> DELEGATE = DataClassRowMapper.newInstance(BanRecord.class);

    @Override
    public BanRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        return DELEGATE.mapRow(rs, rowNum);
    }
}