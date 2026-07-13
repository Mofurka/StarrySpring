package irden.space.proxy.plugin.site.persistence.mapper;

import irden.space.proxy.plugin.site.persistence.model.PlayerAttributesRecord;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class PlayerAttributesRowMapper implements RowMapper<PlayerAttributesRecord> {

    private static final RowMapper<PlayerAttributesRecord> DELEGATE = DataClassRowMapper.newInstance(PlayerAttributesRecord.class);

    @Override
    public PlayerAttributesRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        return DELEGATE.mapRow(rs, rowNum);
    }
}
