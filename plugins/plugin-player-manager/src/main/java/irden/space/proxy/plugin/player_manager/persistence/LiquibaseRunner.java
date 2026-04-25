package irden.space.proxy.plugin.player_manager.persistence;

import irden.space.proxy.plugin.api.PluginLiquibaseRunner;

import javax.sql.DataSource;

public final class LiquibaseRunner {

    private LiquibaseRunner() {
        // Private constructor to prevent instantiation
    }

    public static void runLiquibaseMigrations(DataSource dataSource) {

        PluginLiquibaseRunner.run(dataSource, "db/changelog/player-manager.xml");
    }
}
