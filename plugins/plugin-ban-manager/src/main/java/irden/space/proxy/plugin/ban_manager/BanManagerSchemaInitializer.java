package irden.space.proxy.plugin.ban_manager;

import irden.space.proxy.plugin.api.annotations.OnLoad;
import irden.space.proxy.plugin.ban_manager.persistence.LiquibaseRunner;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class BanManagerSchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(BanManagerSchemaInitializer.class);

    private final DataSource dataSource;

    @OnLoad
    public void migrate() {
        log.info("Running ban-manager database migrations");
        LiquibaseRunner.runLiquibaseMigrations(dataSource);
    }
}
