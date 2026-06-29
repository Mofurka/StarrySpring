package irden.space.proxy.plugin.player_manager;

import irden.space.proxy.plugin.api.annotations.OnLoad;
import irden.space.proxy.plugin.player_manager.persistence.LiquibaseRunner;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
@RequiredArgsConstructor
public class PlayerManagerSchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(PlayerManagerSchemaInitializer.class);

    private final DataSource dataSource;

    @OnLoad
    public void migrate() {
        log.info("Running player-manager database migrations");
        LiquibaseRunner.runLiquibaseMigrations(dataSource);
    }
}
