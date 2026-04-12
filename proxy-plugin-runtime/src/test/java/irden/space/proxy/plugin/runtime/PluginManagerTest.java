package irden.space.proxy.plugin.runtime;

import irden.space.proxy.plugin.api.PacketInterceptor;
import irden.space.proxy.plugin.api.PacketInterceptorRegistry;
import irden.space.proxy.plugin.api.PluginContext;
import irden.space.proxy.plugin.api.PluginDescriptor;
import irden.space.proxy.plugin.api.ProxyPlugin;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PluginManagerTest {

    @Test
    void loadAndStartLogsAgainstResolvedDependencyOrder() {
        List<String> lifecycleEvents = new ArrayList<>();

        TestPlugin corePlugin = new TestPlugin("core", List.of(), lifecycleEvents);
        TestPlugin featurePlugin = new TestPlugin("feature", List.of("core"), lifecycleEvents);

        PluginLoader pluginLoader = new PluginLoader() {
            @Override
            public List<ProxyPlugin> loadPlugins() {
                return List.of(featurePlugin, corePlugin);
            }
        };

        PacketInterceptorRegistry registry = new PacketInterceptorRegistry() {
            private final List<PacketInterceptor> interceptors = new ArrayList<>();

            @Override
            public void register(PacketInterceptor interceptor) {
                interceptors.add(interceptor);
            }

            @Override
            public List<PacketInterceptor> getAll() {
                return List.copyOf(interceptors);
            }
        };

        PluginContext pluginContext = () -> registry;

        PluginManager manager = new PluginManager(
                pluginLoader,
                new PluginDependencyResolver(),
                registry,
                pluginContext
        );

        manager.loadAndStart();

        assertEquals(
                List.of("load:core", "load:feature", "start:core", "start:feature"),
                lifecycleEvents
        );
        assertEquals(List.of(corePlugin, featurePlugin), manager.getLoadedPlugins());

        manager.stopAll();

        assertEquals(
                List.of(
                        "load:core",
                        "load:feature",
                        "start:core",
                        "start:feature",
                        "stop:feature",
                        "stop:core"
                ),
                lifecycleEvents
        );
    }

    private static final class TestPlugin implements ProxyPlugin {

        private final PluginDescriptor descriptor;
        private final List<String> lifecycleEvents;

        private TestPlugin(String id, List<String> dependsOn, List<String> lifecycleEvents) {
            this.descriptor = new PluginDescriptor(id, id, "1.0.0", dependsOn);
            this.lifecycleEvents = lifecycleEvents;
        }

        @Override
        public PluginDescriptor descriptor() {
            return descriptor;
        }

        @Override
        public void onLoad(PluginContext context) {
            lifecycleEvents.add("load:" + descriptor.id());
        }

        @Override
        public void onStart() {
            lifecycleEvents.add("start:" + descriptor.id());
        }

        @Override
        public void onStop() {
            lifecycleEvents.add("stop:" + descriptor.id());
        }
    }
}

