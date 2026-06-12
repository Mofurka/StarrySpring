package irden.space.boot;

import irden.space.proxy.plugin.command_handler.CommandHandlerPlugin;
import irden.space.proxy.plugin.runtime.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ServiceLoaderPluginCandidateTest {

    @Test
    void discoversClasspathPluginClassWithoutInstantiatingEntryPoint() {
        PluginCandidate candidate = new ServiceLoaderPluginLoader().loadPluginCandidates().stream()
                .filter(plugin -> plugin.pluginClass().equals(CommandHandlerPlugin.class))
                .findFirst()
                .orElseThrow();
        DefaultPluginContext pluginContext = new DefaultPluginContext(new DefaultPacketInterceptorRegistry());
        AnnotationConfigApplicationContext rootContext = new AnnotationConfigApplicationContext();
        rootContext.refresh();

        PluginContainer container = new SpringPluginContainerFactory(rootContext, pluginContext).create(
                candidate,
                pluginContext.forPlugin(candidate.descriptor().id())
        );

        assertInstanceOf(CommandHandlerPlugin.class, container.plugin());

        container.close();
        rootContext.close();
    }
}
