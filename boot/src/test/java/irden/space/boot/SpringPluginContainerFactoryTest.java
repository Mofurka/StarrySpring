package irden.space.boot;

import irden.space.boot.pluginfixture.ComponentScannedPlugin;
import irden.space.proxy.plugin.api.*;
import irden.space.proxy.plugin.runtime.DefaultPacketInterceptorRegistry;
import irden.space.proxy.plugin.runtime.DefaultPluginContext;
import irden.space.proxy.plugin.runtime.PluginContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void injectsServicesPublishedByDeclaredPluginDependencies() {
        AnnotationConfigApplicationContext rootContext = new AnnotationConfigApplicationContext();
        rootContext.refresh();
        DefaultPluginContext pluginContext = new DefaultPluginContext(new DefaultPacketInterceptorRegistry());
        SharedPluginService service = new SharedPluginService();
        pluginContext.forPlugin("provider").publishService(SharedPluginService.class, service);
        DependencyConsumerPlugin plugin = new DependencyConsumerPlugin();

        PluginContainer container = new SpringPluginContainerFactory(rootContext, pluginContext).create(plugin);

        assertSame(service, plugin.service);

        container.close();
        assertTrue(!service.closed);
        rootContext.close();
    }

    @Test
    void doesNotInjectServicesFromUndeclaredPluginDependencies() {
        AnnotationConfigApplicationContext rootContext = new AnnotationConfigApplicationContext();
        rootContext.refresh();
        DefaultPluginContext pluginContext = new DefaultPluginContext(new DefaultPacketInterceptorRegistry());
        pluginContext.forPlugin("provider").publishService(SharedPluginService.class, new SharedPluginService());

        assertThrows(
                RuntimeException.class,
                () -> new SpringPluginContainerFactory(rootContext, pluginContext).create(new UndeclaredDependencyConsumerPlugin())
        );

        rootContext.close();
    }

    @Test
    void injectsOwnerScopedRuntimeBeansInsteadOfRootBeans() {
        DefaultPluginContext rootPluginContext = new DefaultPluginContext(new DefaultPacketInterceptorRegistry());
        PluginContext scopedContext = rootPluginContext.forPlugin("scoped");
        AnnotationConfigApplicationContext rootContext = new AnnotationConfigApplicationContext();
        rootContext.registerBean(PluginContext.class, () -> rootPluginContext);
        rootContext.registerBean(PacketInterceptorRegistry.class, rootPluginContext::packetInterceptorRegistry);
        rootContext.refresh();
        ScopedContextPlugin plugin = new ScopedContextPlugin();

        PluginContainer container = new SpringPluginContainerFactory(rootContext).create(plugin, scopedContext);

        assertSame(scopedContext, plugin.pluginContext);
        assertSame(scopedContext.packetInterceptorRegistry(), plugin.interceptorRegistry);

        container.close();
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

    static final class SharedPluginService implements AutoCloseable {
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

    @PluginSpringConfiguration(scanPluginPackage = false)
    static final class DependencyConsumerPlugin implements ProxyPlugin {
        @Autowired
        private SharedPluginService service;

        @Override
        public PluginDescriptor descriptor() {
            return new PluginDescriptor("consumer", "Consumer", "1.0.0", List.of("provider"));
        }
    }

    @PluginSpringConfiguration(scanPluginPackage = false)
    static final class UndeclaredDependencyConsumerPlugin implements ProxyPlugin {
        @Autowired
        private SharedPluginService service;

        @Override
        public PluginDescriptor descriptor() {
            return new PluginDescriptor("consumer", "Consumer", "1.0.0", List.of());
        }
    }

    @PluginSpringConfiguration(scanPluginPackage = false)
    static final class ScopedContextPlugin implements ProxyPlugin {
        @Autowired
        private PluginContext pluginContext;

        @Autowired
        private PacketInterceptorRegistry interceptorRegistry;

        @Override
        public PluginDescriptor descriptor() {
            return new PluginDescriptor("scoped", "Scoped", "1.0.0", List.of());
        }
    }
}
