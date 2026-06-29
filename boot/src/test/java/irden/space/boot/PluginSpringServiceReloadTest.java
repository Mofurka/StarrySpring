package irden.space.boot;

import irden.space.proxy.plugin.api.PluginDescriptor;
import irden.space.proxy.plugin.api.PluginSpringConfiguration;
import irden.space.proxy.plugin.api.ProxyPlugin;
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
                new SpringPluginContainerFactory(rootContext)
        );

        manager.loadAndStart();
        ConsumerPlugin firstConsumer = loadedConsumer(manager);
        ProviderPlugin firstProvider = loadedProvider(manager, ProviderPlugin.class);
        assertSame(firstProvider, firstConsumer.service);

        manager.reloadPlugin("provider");

        ConsumerPlugin secondConsumer = loadedConsumer(manager);
        ReloadedProviderPlugin secondProvider = loadedProvider(manager, ReloadedProviderPlugin.class);
        assertSame(secondProvider, secondConsumer.service);
        assertNotSame(firstConsumer.service, secondConsumer.service);

        manager.stopAll();
        rootContext.close();
    }

    interface SharedService {
        String value();
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

    private static <T extends ProxyPlugin> T loadedProvider(PluginManager manager, Class<T> type) {
        return manager.getLoadedPlugins().stream()
                .filter(type::isInstance)
                .map(type::cast)
                .findFirst()
                .orElseThrow();
    }

    @PluginSpringConfiguration(scanPluginPackage = false)
    static final class ProviderPlugin implements ProxyPlugin, SharedService {
        @Override
        public PluginDescriptor descriptor() {
            return new PluginDescriptor("provider", "Provider", "1.0.0", List.of());
        }

        @Override
        public String value() {
            return "first";
        }
    }

    @PluginSpringConfiguration(scanPluginPackage = false)
    static final class ReloadedProviderPlugin implements ProxyPlugin, SharedService {
        @Override
        public PluginDescriptor descriptor() {
            return new PluginDescriptor("provider", "Provider", "1.0.0", List.of());
        }

        @Override
        public String value() {
            return "second";
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
