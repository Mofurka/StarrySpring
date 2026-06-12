package irden.space.boot;

import irden.space.proxy.plugin.discord.DiscordBotPlugin;
import irden.space.proxy.plugin.runtime.PluginContainer;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertSame;

class DiscordBotSpringWiringTest {

    @Test
    void injectsDiscordPluginInfrastructureWithoutStartingJda() {
        AnnotationConfigApplicationContext rootContext = new AnnotationConfigApplicationContext();
        rootContext.refresh();
        DiscordBotPlugin plugin = new DiscordBotPlugin();

        PluginContainer container = new SpringPluginContainerFactory(rootContext).create(plugin);

        assertSame(plugin, container.plugin());

        container.close();
        rootContext.close();
    }
}
