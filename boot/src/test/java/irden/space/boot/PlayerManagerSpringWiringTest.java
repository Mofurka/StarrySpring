package irden.space.boot;

import irden.space.proxy.plugin.api.PermissionView;
import irden.space.proxy.plugin.api.SessionPermissionService;
import irden.space.proxy.plugin.ban_manager.BanManagerPlugin;
import irden.space.proxy.plugin.command_handler.CommandHandlerPlugin;
import irden.space.proxy.plugin.player_manager.PlayerManagerPlugin;
import irden.space.proxy.plugin.runtime.DefaultPacketInterceptorRegistry;
import irden.space.proxy.plugin.runtime.DefaultPluginContext;
import irden.space.proxy.plugin.runtime.PluginCandidate;
import irden.space.proxy.plugin.runtime.PluginContainer;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.AbstractDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class PlayerManagerSpringWiringTest {

    @Test
    void createsPlayerManagerAndBanManagerWithSpringManagedInfrastructure() {
        AnnotationConfigApplicationContext rootContext = rootContext();
        DefaultPluginContext pluginContext = new DefaultPluginContext(new DefaultPacketInterceptorRegistry());
        SpringPluginContainerFactory containerFactory = new SpringPluginContainerFactory(rootContext);
        PluginContainer commandHandlerContainer = containerFactory.create(
                PluginCandidate.fromClass(CommandHandlerPlugin.class),
                pluginContext.forPlugin("command-handler"),
                List.of()
        );
        PluginContainer playerManagerContainer = containerFactory.create(
                PluginCandidate.fromClass(PlayerManagerPlugin.class),
                pluginContext.forPlugin("player-manager"),
                List.of(commandHandlerContainer)
        );
        PluginContainer banManagerContainer = containerFactory.create(
                PluginCandidate.fromClass(BanManagerPlugin.class),
                pluginContext.forPlugin("ban-manager"),
                List.of(playerManagerContainer)
        );

        assertInstanceOf(PlayerManagerPlugin.class, playerManagerContainer.plugin());
        assertInstanceOf(BanManagerPlugin.class, banManagerContainer.plugin());

        banManagerContainer.close();
        playerManagerContainer.close();
        commandHandlerContainer.close();
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
