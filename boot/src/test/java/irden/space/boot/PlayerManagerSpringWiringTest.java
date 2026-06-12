package irden.space.boot;

import irden.space.proxy.plugin.api.PermissionView;
import irden.space.proxy.plugin.api.SessionPermissionService;
import irden.space.proxy.plugin.player_manager.BanManagerPlugin;
import irden.space.proxy.plugin.player_manager.PlayerManagerPlugin;
import irden.space.proxy.plugin.runtime.PluginContainer;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.AbstractDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertSame;

class PlayerManagerSpringWiringTest {

    @Test
    void createsPlayerManagerAndBanManagerWithSpringManagedInfrastructure() {
        AnnotationConfigApplicationContext rootContext = rootContext();
        PlayerManagerPlugin playerManager = new PlayerManagerPlugin();
        BanManagerPlugin banManager = new BanManagerPlugin();

        PluginContainer playerManagerContainer = new SpringPluginContainerFactory(rootContext).create(playerManager);
        PluginContainer banManagerContainer = new SpringPluginContainerFactory(rootContext).create(banManager);

        assertSame(playerManager, playerManagerContainer.plugin());
        assertSame(banManager, banManagerContainer.plugin());

        banManagerContainer.close();
        playerManagerContainer.close();
        rootContext.close();
    }

    private AnnotationConfigApplicationContext rootContext() {
        DataSource dataSource = new NonConnectingDataSource();
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.registerBean(DataSource.class, () -> dataSource);
        context.registerBean(JdbcTemplate.class, () -> new JdbcTemplate(dataSource));
        context.registerBean(SessionPermissionService.class, EmptySessionPermissionService::new);
        context.refresh();
        return context;
    }

    private static final class NonConnectingDataSource extends AbstractDataSource {
        @Override
        public Connection getConnection() throws SQLException {
            throw new SQLException("Connections are not expected during Spring wiring");
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            throw new SQLException("Connections are not expected during Spring wiring");
        }
    }

    private static final class EmptySessionPermissionService implements SessionPermissionService {
        @Override
        public PermissionView permissions(String sessionId) {
            return PermissionView.EMPTY;
        }

        @Override
        public void updatePermissions(String sessionId, PermissionView permissions) {
        }

        @Override
        public void clearPermissions(String sessionId) {
        }
    }
}
