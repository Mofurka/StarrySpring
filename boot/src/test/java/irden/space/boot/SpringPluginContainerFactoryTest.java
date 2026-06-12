package irden.space.boot;

import irden.space.boot.pluginfixture.ComponentScannedPlugin;
import irden.space.proxy.plugin.api.PluginContext;
import irden.space.proxy.plugin.api.PluginDescriptor;
import irden.space.proxy.plugin.api.PluginSpringConfiguration;
import irden.space.proxy.plugin.api.ProxyPlugin;
import irden.space.proxy.plugin.runtime.PluginContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringPluginContainerFactoryTest {

    @Test
    void scansPluginPackageForComponentsByDefault() {
        AnnotationConfigApplicationContext rootContext = new AnnotationConfigApplicationContext();
        rootContext.refresh();

        ComponentScannedPlugin plugin = new ComponentScannedPlugin();
        PluginContainer container = new SpringPluginContainerFactory(rootContext).create(plugin);

        assertSame(plugin, container.plugin());
        assertTrue(plugin.dependency() != null);

        container.close();
        rootContext.close();
    }

    @Test
    void createsChildContextWithRootDependenciesAndClosesPluginBeans() {
        RootDependency rootDependency = new RootDependency();
        AnnotationConfigApplicationContext rootContext = new AnnotationConfigApplicationContext();
        rootContext.registerBean(RootDependency.class, () -> rootDependency);
        rootContext.refresh();

        SpringManagedPlugin plugin = new SpringManagedPlugin();
        PluginContainer container = new SpringPluginContainerFactory(rootContext).create(plugin);

        assertSame(plugin, container.plugin());
        assertSame(rootDependency, plugin.rootDependency);
        assertTrue(!plugin.resource.closed);

        container.close();

        assertTrue(plugin.resource.closed);
        rootContext.close();
    }

    static final class RootDependency {
    }

    static final class PluginOwnedResource implements AutoCloseable {
        private boolean closed;

        @Override
        public void close() {
            closed = true;
        }
    }

    static final class PluginConfiguration {
        @Bean
        PluginOwnedResource pluginOwnedResource() {
            return new PluginOwnedResource();
        }
    }

    @PluginSpringConfiguration(value = PluginConfiguration.class, scanPluginPackage = false)
    static final class SpringManagedPlugin implements ProxyPlugin {
        @Autowired
        private RootDependency rootDependency;

        @Autowired
        private PluginOwnedResource resource;

        @Override
        public PluginDescriptor descriptor() {
            return new PluginDescriptor("spring-test", "Spring Test", "1.0.0", List.of());
        }

        @Override
        public void onLoad(PluginContext context) {
        }
    }
}
