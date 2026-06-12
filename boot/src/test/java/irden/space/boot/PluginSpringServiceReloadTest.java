package irden.space.boot;

import irden.space.proxy.plugin.api.PluginDescriptor;
import irden.space.proxy.plugin.api.PluginSpringConfiguration;
import irden.space.proxy.plugin.api.ProxyPlugin;
import irden.space.proxy.plugin.api.annotations.PublishService;
import irden.space.proxy.plugin.runtime.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class PluginSpringServiceReloadTest {

    @Test
    void reinjectsReloadedDependencyServiceIntoDependentPlugin() {
        AtomicReference<List<PluginCandidate>> availablePlugins = new AtomicReference<>(
                candidates(ProviderPlugin.class)
        );
        PluginLoader loader = new PluginLoader() {
            @Override
            public List<PluginCandidate> loadPluginCandidates() {
                return availablePlugins.get();
            }

            @Override
            public void reloadPluginCandidates(List<PluginCandidate> plugins) {
                availablePlugins.set(candidates(ReloadedProviderPlugin.class));
            }
        };
        DefaultPacketInterceptorRegistry registry = new DefaultPacketInterceptorRegistry();
        DefaultPluginContext pluginContext = new DefaultPluginContext(registry);
        AnnotationConfigApplicationContext rootContext = new AnnotationConfigApplicationContext();
        rootContext.refresh();
        PluginManager manager = new PluginManager(
                loader,
                new PluginDependencyResolver(),
                registry,
                pluginContext,
                new SpringPluginContainerFactory(rootContext, pluginContext)
        );

        manager.loadAndStart();
        ConsumerPlugin firstConsumer = loadedConsumer(manager);
        assertSame(ProviderPlugin.SERVICE, firstConsumer.service);

        manager.reloadPlugin("provider");

        ConsumerPlugin secondConsumer = loadedConsumer(manager);
        assertSame(ReloadedProviderPlugin.SERVICE, secondConsumer.service);
        assertNotSame(firstConsumer.service, secondConsumer.service);

        manager.stopAll();
        rootContext.close();
    }

    record SharedService(String value) {
    }

    private static List<PluginCandidate> candidates(Class<? extends ProxyPlugin> providerClass) {
        PluginDescriptor providerDescriptor = new PluginDescriptor("provider", "Provider", "1.0.0", List.of());
        PluginDescriptor consumerDescriptor = new PluginDescriptor("consumer", "Consumer", "1.0.0", List.of("provider"));
        return List.of(
                new PluginCandidate(ConsumerPlugin.class, consumerDescriptor),
                new PluginCandidate(providerClass, providerDescriptor)
        );
    }

    private static ConsumerPlugin loadedConsumer(PluginManager manager) {
        return manager.getLoadedPlugins().stream()
                .filter(ConsumerPlugin.class::isInstance)
                .map(ConsumerPlugin.class::cast)
                .findFirst()
                .orElseThrow();
    }

    @PluginSpringConfiguration(scanPluginPackage = false)
    static final class ProviderPlugin implements ProxyPlugin {
        private static final SharedService SERVICE = new SharedService("first");

        @Override
        public PluginDescriptor descriptor() {
            return new PluginDescriptor("provider", "Provider", "1.0.0", List.of());
        }

        @PublishService
        public SharedService publishService() {
            return SERVICE;
        }
    }

    @PluginSpringConfiguration(scanPluginPackage = false)
    static final class ReloadedProviderPlugin implements ProxyPlugin {
        private static final SharedService SERVICE = new SharedService("second");

        @Override
        public PluginDescriptor descriptor() {
            return new PluginDescriptor("provider", "Provider", "1.0.0", List.of());
        }

        @PublishService
        public SharedService publishService() {
            return SERVICE;
        }
    }

    @PluginSpringConfiguration(scanPluginPackage = false)
    static final class ConsumerPlugin implements ProxyPlugin {
        @Autowired
        private SharedService service;

        @Override
        public PluginDescriptor descriptor() {
            return new PluginDescriptor("consumer", "Consumer", "1.0.0", List.of("provider"));
        }
    }
}
