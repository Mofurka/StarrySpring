package irden.space.proxy.plugin.site.persistence.liquibase;

import irden.space.proxy.plugin.api.PluginLiquibaseRunner;
import irden.space.proxy.plugin.api.annotations.OnLoad;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
@RequiredArgsConstructor
@Slf4j
public class SiteConnectorSchemaInitializer {
    private final DataSource dataSource;

    @OnLoad
    public void migrate() {
        log.info("Running site connector database migrations");
        PluginLiquibaseRunner.run(dataSource, "db/changelog/site-connector-changelog.xml");
    }
}
