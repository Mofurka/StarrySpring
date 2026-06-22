package irden.space.boot;

import irden.space.proxy.plugin.api.ProxyPlugin;
import irden.space.proxy.plugin.command_handler.CommandHandlerPlugin;
import irden.space.proxy.plugin.command_handler.CommandParser;
import irden.space.proxy.plugin.discord.DiscordBotPlugin;
import irden.space.proxy.plugin.player_manager.permissions.PermissionResolver;
import irden.space.proxy.plugin.player_manager.roles.RoleManager;
import irden.space.proxy.plugin.runtime.DefaultPacketInterceptorRegistry;
import irden.space.proxy.plugin.runtime.DefaultPluginContext;
import irden.space.proxy.plugin.runtime.PluginCandidate;
import irden.space.proxy.plugin.runtime.PluginContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class DiscordBotSpringWiringTest {

    @Test
    void injectsDiscordPluginInfrastructureWithoutStartingJda(@TempDir Path tempDir) {
        AnnotationConfigApplicationContext rootContext = new AnnotationConfigApplicationContext();
        rootContext.refresh();
        DefaultPluginContext pluginContext = new DefaultPluginContext(new DefaultPacketInterceptorRegistry());
        PluginContainer commandHandlerContainer = dependencyContainer(
                "command-handler",
                Map.of("commandHandlerPlugin", new CommandHandlerPlugin(new CommandParser()))
        );
        PluginContainer playerManagerContainer = dependencyContainer(
                "player-manager",
                Map.of("roleManager", new RoleManager(tempDir.resolve("permissions.jsonc"), new PermissionResolver()))
        );
        PluginContainer container = new SpringPluginContainerFactory(rootContext).create(
                PluginCandidate.fromClass(DiscordBotPlugin.class),
                pluginContext.forPlugin("discord-bot"),
                List.of(commandHandlerContainer, playerManagerContainer)
        );

        assertInstanceOf(DiscordBotPlugin.class, container.plugin());

        container.close();
        rootContext.close();
    }

    private static PluginContainer dependencyContainer(String pluginId, Map<String, Object> beans) {
        ProxyPlugin plugin = new ProxyPlugin() {
            @Override
            public irden.space.proxy.plugin.api.PluginDescriptor descriptor() {
                return new irden.space.proxy.plugin.api.PluginDescriptor(pluginId, pluginId, "1.0.0", List.of());
            }
        };
        return new PluginContainer() {
            @Override
            public ProxyPlugin plugin() {
                return plugin;
            }

            @Override
            public <T> Map<String, T> beansOfType(Class<T> type) {
                return beans.entrySet().stream()
                        .filter(entry -> type.isInstance(entry.getValue()))
                        .collect(java.util.stream.Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> type.cast(entry.getValue())
                        ));
            }

            @Override
            public void close() {
            }
        };
    }
}
