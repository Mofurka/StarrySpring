package irden.space.boot;

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

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class DiscordBotSpringWiringTest {

    @Test
    void injectsDiscordPluginInfrastructureWithoutStartingJda(@TempDir Path tempDir) {
        AnnotationConfigApplicationContext rootContext = new AnnotationConfigApplicationContext();
        rootContext.refresh();
        DefaultPluginContext pluginContext = new DefaultPluginContext(new DefaultPacketInterceptorRegistry());
        pluginContext.forPlugin("command-handler").publishService(
                CommandHandlerPlugin.class,
                new CommandHandlerPlugin(new CommandParser())
        );
        pluginContext.forPlugin("player-manager").publishService(
                RoleManager.class,
                new RoleManager(tempDir.resolve("permissions.jsonc"), new PermissionResolver())
        );
        PluginContainer container = new SpringPluginContainerFactory(rootContext, pluginContext).create(
                PluginCandidate.fromClass(DiscordBotPlugin.class),
                pluginContext.forPlugin("discord-bot")
        );

        assertInstanceOf(DiscordBotPlugin.class, container.plugin());

        container.close();
        rootContext.close();
    }
}
