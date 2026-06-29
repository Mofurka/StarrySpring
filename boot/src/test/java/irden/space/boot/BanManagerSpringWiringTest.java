package irden.space.boot;

import irden.space.proxy.plugin.api.PermissionView;
import irden.space.proxy.plugin.api.SessionPermissionService;
import irden.space.proxy.plugin.ban_manager.BanManagerPlugin;
import irden.space.proxy.plugin.command_handler.CommandHandlerPlugin;
import irden.space.proxy.plugin.command_handler.CommandRegistry;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that the ban-manager entry point delegates registration to its dedicated Spring
 * components: commands come from {@code BanCommands} and the connection packet handler comes from
 * {@code BanConnectionHandler}, even though {@code BanManagerPlugin} itself declares neither.
 */
class BanManagerSpringWiringTest {

    @Test
    void registersBanCommandsAndConnectionHandlerFromDedicatedComponents() {
        DefaultPacketInterceptorRegistry registry = new DefaultPacketInterceptorRegistry();
        DefaultPluginContext pluginContext = new DefaultPluginContext(registry);
        AnnotationConfigApplicationContext rootContext = rootContext();
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

        assertEquals(0, registry.getAll().size());

        banManagerContainer.registerAnnotatedBeans();

        assertNotNull(CommandRegistry.global().find("ban"), "ban command should be registered");
        assertNotNull(CommandRegistry.global().find("kick"), "kick command should be registered");
        assertNotNull(CommandRegistry.global().find("unban"), "unban command should be registered");
        assertEquals(1, registry.getAll().size(), "connection packet handler should be registered");

        pluginContext.removePlugin("ban-manager");

        assertNull(CommandRegistry.global().find("ban"), "ban command should be removed on stop");
        assertNull(CommandRegistry.global().find("kick"), "kick command should be removed on stop");
        assertNull(CommandRegistry.global().find("unban"), "unban command should be removed on stop");
        assertEquals(0, registry.getAll().size(), "connection packet handler should be removed on stop");

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
