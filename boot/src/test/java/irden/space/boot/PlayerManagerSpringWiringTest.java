package irden.space.boot;

import irden.space.proxy.plugin.api.PermissionView;
import irden.space.proxy.plugin.api.SessionPermissionService;
import irden.space.proxy.plugin.command_handler.CommandHandlerPlugin;
import irden.space.proxy.plugin.command_handler.CommandParser;
import irden.space.proxy.plugin.ban_manager.BanManagerPlugin;
import irden.space.proxy.plugin.player_manager.PlayerManagerPlugin;
import irden.space.proxy.plugin.player_manager.api.PlayerManagerApi;
import irden.space.proxy.plugin.player_manager.model.Player;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class PlayerManagerSpringWiringTest {

    @Test
    void createsPlayerManagerAndBanManagerWithSpringManagedInfrastructure() {
        AnnotationConfigApplicationContext rootContext = rootContext();
        DefaultPluginContext pluginContext = new DefaultPluginContext(new DefaultPacketInterceptorRegistry());
        pluginContext.forPlugin("command-handler").publishService(
                CommandHandlerPlugin.class,
                new CommandHandlerPlugin(new CommandParser())
        );
        pluginContext.forPlugin("player-manager").publishService(PlayerManagerApi.class, new EmptyPlayerManagerApi());

        SpringPluginContainerFactory containerFactory = new SpringPluginContainerFactory(rootContext, pluginContext);
        PluginContainer playerManagerContainer = containerFactory.create(
                PluginCandidate.fromClass(PlayerManagerPlugin.class),
                pluginContext.forPlugin("player-manager")
        );
        PluginContainer banManagerContainer = containerFactory.create(
                PluginCandidate.fromClass(BanManagerPlugin.class),
                pluginContext.forPlugin("ban-manager")
        );

        assertInstanceOf(PlayerManagerPlugin.class, playerManagerContainer.plugin());
        assertInstanceOf(BanManagerPlugin.class, banManagerContainer.plugin());

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

    private static final class EmptyPlayerManagerApi implements PlayerManagerApi {
        @Override
        public Optional<Player> findPlayer(String identifier, boolean loggedIn) {
            return Optional.empty();
        }

        @Override
        public List<Player> searchPlayers(String prefix, int limit, boolean loggedIn) {
            return List.of();
        }

        @Override
        public List<Player> findAllPlayersByIpAddress(String ipAddress) {
            return List.of();
        }

        @Override
        public Optional<Player> getPlayerBySessionId(String sessionId) {
            return Optional.empty();
        }
    }
}
