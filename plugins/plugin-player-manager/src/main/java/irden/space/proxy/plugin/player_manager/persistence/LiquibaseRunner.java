package irden.space.proxy.plugin.player_manager.persistence;

import irden.space.proxy.plugin.api.PluginLiquibaseRunner;
import lombok.experimental.UtilityClass;

import javax.sql.DataSource;

@UtilityClass
public final class LiquibaseRunner {


    public static void runLiquibaseMigrations(DataSource dataSource) {

        PluginLiquibaseRunner.run(dataSource, "db/changelog/player-manager.xml");
    }
}
