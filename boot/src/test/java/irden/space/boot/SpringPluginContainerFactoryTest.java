package irden.space.boot;

import irden.space.boot.pluginfixture.ComponentScannedPlugin;
import irden.space.proxy.plugin.api.*;
import irden.space.proxy.plugin.runtime.DefaultPacketInterceptorRegistry;
import irden.space.proxy.plugin.runtime.DefaultPluginContext;
import irden.space.proxy.plugin.runtime.PluginCandidate;
import irden.space.proxy.plugin.runtime.PluginContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SpringPluginContainerFactoryTest {

    @Test
    void scansPluginPackageForComponentsByDefault() {
        AnnotationConfigApplicationContext rootContext = new AnnotationConfigApplicationContext();
        rootContext.refresh();

        ComponentScannedPlugin descriptorSource = new ComponentScannedPlugin();
        PluginContainer container = new SpringPluginContainerFactory(rootContext).create(
                candidate(ComponentScannedPlugin.class, descriptorSource.descriptor()),
                null,
                List.of()
        );
        ComponentScannedPlugin plugin = (ComponentScannedPlugin) container.plugin();

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

        PluginContainer container = new SpringPluginContainerFactory(rootContext).create(
                candidate(SpringManagedPlugin.class, new SpringManagedPlugin().descriptor()),
                null,
                List.of()
        );
        SpringManagedPlugin plugin = (SpringManagedPlugin) container.plugin();

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
        PluginContainer provider = dependencyContainer(
                "provider",
                Map.of("sharedPluginService", service)
        );
        PluginContainer container = new SpringPluginContainerFactory(rootContext).create(
                candidate(DependencyConsumerPlugin.class, new DependencyConsumerPlugin().descriptor()),
                pluginContext.forPlugin("consumer"),
                List.of(provider)
        );
        DependencyConsumerPlugin plugin = (DependencyConsumerPlugin) container.plugin();

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

        assertThrows(
                RuntimeException.class,
                () -> new SpringPluginContainerFactory(rootContext).create(
                        candidate(
                                UndeclaredDependencyConsumerPlugin.class,
                                new UndeclaredDependencyConsumerPlugin().descriptor()
                        ),
                        pluginContext.forPlugin("consumer"),
                        List.of()
                )
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
        PluginContainer container = new SpringPluginContainerFactory(rootContext).create(
                candidate(ScopedContextPlugin.class, new ScopedContextPlugin().descriptor()),
                scopedContext,
                List.of()
        );
        ScopedContextPlugin plugin = (ScopedContextPlugin) container.plugin();

        assertSame(scopedContext, plugin.pluginContext);
        assertSame(scopedContext.packetInterceptorRegistry(), plugin.interceptorRegistry);

        container.close();
        rootContext.close();
    }

    static final class RootDependency {
    }

    private static PluginCandidate candidate(
            Class<? extends ProxyPlugin> pluginClass,
            PluginDescriptor descriptor
    ) {
        return new PluginCandidate(pluginClass, descriptor);
    }

    @Test
    void doesNotExposeSpringInfrastructureBeansFromDependencyContexts() {
        AnnotationConfigApplicationContext rootContext = new AnnotationConfigApplicationContext();
        rootContext.refresh();
        DefaultPluginContext pluginContext = new DefaultPluginContext(new DefaultPacketInterceptorRegistry());
        SpringPluginContainerFactory factory = new SpringPluginContainerFactory(rootContext);
        PluginContainer provider = factory.create(
                candidate(DependencyProviderPlugin.class, new DependencyProviderPlugin().descriptor()),
                pluginContext.forPlugin("provider"),
                List.of()
        );
        PluginContainer consumer = factory.create(
                candidate(DependencyConsumerPlugin.class, new DependencyConsumerPlugin().descriptor()),
                pluginContext.forPlugin("consumer"),
                List.of(provider)
        );

        DependencyConsumerPlugin plugin = (DependencyConsumerPlugin) consumer.plugin();

        SharedPluginService service = provider.beansOfType(SharedPluginService.class)
                .values()
                .stream()
                .findFirst()
                .orElseThrow();
        assertSame(service, plugin.service);
        assertTrue(provider.beansOfType(BeanFactoryPostProcessor.class).isEmpty());

        consumer.close();
        provider.close();
        rootContext.close();
    }

    private static PluginContainer dependencyContainer(String pluginId, Map<String, Object> beans) {
        ProxyPlugin plugin = new ProxyPlugin() {
            @Override
            public PluginDescriptor descriptor() {
                return new PluginDescriptor(pluginId, pluginId, "1.0.0", List.of());
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

    static final class DependencyProviderConfiguration {
        @Bean
        SharedPluginService sharedPluginService() {
            return new SharedPluginService();
        }
    }

    @PluginSpringConfiguration(value = DependencyProviderConfiguration.class, scanPluginPackage = false)
    static final class DependencyProviderPlugin implements ProxyPlugin {
        @Override
        public PluginDescriptor descriptor() {
            return new PluginDescriptor("provider", "Provider", "1.0.0", List.of());
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
