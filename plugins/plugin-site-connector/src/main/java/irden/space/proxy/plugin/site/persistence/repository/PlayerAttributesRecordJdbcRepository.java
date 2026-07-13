package irden.space.proxy.plugin.site.persistence.repository;

import irden.space.proxy.plugin.site.persistence.mapper.PlayerAttributesRowMapper;
import irden.space.proxy.plugin.site.persistence.model.PlayerAttributesRecord;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.intellij.lang.annotations.Language;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PlayerAttributesRecordJdbcRepository {
    private final PlayerAttributesRowMapper rowMapper;
    private final JdbcTemplate jdbcTemplate;

    public Optional<PlayerAttributesRecord> findByPlayerUuid(@NotBlank String playerUuid) {
        @Language("sql")
        var sql = "SELECT * FROM site_connector.player_attributes WHERE player_uuid = ?";
        List<PlayerAttributesRecord> query = jdbcTemplate.query(sql, rowMapper, playerUuid);
        return Optional.ofNullable(query.isEmpty() ? null : query.getFirst());
    }


}
