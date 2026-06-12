package irden.space.proxy.plugin.player_manager;

import irden.space.proxy.plugin.player_manager.persistence.BanRecordJdbcRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.AbstractDataSource;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerManagerSpringConfigurationTest {

    @Test
    void keepsPlayerManagerAndBanManagerBeanGraphsIsolated(@TempDir Path tempDir) {
        AnnotationConfigApplicationContext playerManagerContext = context(PlayerManagerSpringConfiguration.class, tempDir);
        AnnotationConfigApplicationContext banManagerContext = context(BanManagerSpringConfiguration.class, tempDir);

        assertTrue(playerManagerContext.getBeansOfType(PlayerDirectory.class).size() == 1);
        assertTrue(playerManagerContext.getBeansOfType(BanRecordJdbcRepository.class).isEmpty());
        assertTrue(banManagerContext.getBeansOfType(BanRecordJdbcRepository.class).size() == 1);
        assertFalse(banManagerContext.containsBeanDefinition("playerDirectory"));

        banManagerContext.close();
        playerManagerContext.close();
    }

    private AnnotationConfigApplicationContext context(Class<?> configuration, Path tempDir) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                "test",
                Map.of("starry.player-manager.permissions-path", tempDir.resolve("permissions.jsonc").toString())
        ));
        context.registerBean(JdbcTemplate.class, () -> new JdbcTemplate(new NonConnectingDataSource()));
        context.register(configuration);
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
}
