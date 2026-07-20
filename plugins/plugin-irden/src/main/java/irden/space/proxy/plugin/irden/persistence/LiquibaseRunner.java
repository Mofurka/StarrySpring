package irden.space.proxy.plugin.irden.persistence;

import irden.space.proxy.plugin.api.PluginLiquibaseRunner;
import irden.space.proxy.plugin.api.annotations.OnLoad;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
@RequiredArgsConstructor
public final class LiquibaseRunner {
    private final DataSource dataSource;


    @OnLoad
    public void runLiquibaseMigrations() {
        PluginLiquibaseRunner.run(dataSource, "db/changelog/0.init-schema.xml");
        PluginLiquibaseRunner.run(dataSource, "db/changelog/1.bank-account.xml");
    }
}
