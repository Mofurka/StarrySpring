package irden.space.boot;

import irden.space.proxy.plugin.api.*;
import irden.space.proxy.plugin.api.annotations.OnLoad;
import irden.space.proxy.plugin.runtime.*;
import irden.space.proxy.protocol.packet.PacketType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PluginScopedContextLifecycleTest {

    @Test
    void removesResourcesRegisteredThroughInjectedScopedRuntimeBeans() {
        DefaultPacketInterceptorRegistry registry = new DefaultPacketInterceptorRegistry();
        DefaultPluginContext pluginContext = new DefaultPluginContext(registry);
        AnnotationConfigApplicationContext rootContext = new AnnotationConfigApplicationContext();
        rootContext.registerBean(PluginContext.class, () -> pluginContext);
        rootContext.registerBean(PacketInterceptorRegistry.class, () -> registry);
        rootContext.refresh();
        PluginManager manager = new PluginManager(
                new PluginLoader() {
                    @Override
                    public List<PluginCandidate> loadPluginCandidates() {
                        return List.of(new PluginCandidate(
                                ScopedLifecyclePlugin.class,
                                new ScopedLifecyclePlugin().descriptor()
                        ));
                    }
                },
                new PluginDependencyResolver(),
                registry,
                pluginContext,
                new SpringPluginContainerFactory(rootContext, pluginContext)
        );

        manager.loadAndStart();
        ScopedLifecyclePlugin plugin = (ScopedLifecyclePlugin) manager.getLoadedPlugins().getFirst();

        assertEquals(1, registry.getAll().size());
        assertFalse(plugin.cleanupCalled);

        manager.stopPlugin("scoped-lifecycle");

        assertTrue(registry.getAll().isEmpty());
        assertTrue(plugin.cleanupCalled);

        manager.stopAll();
        rootContext.close();
    }

    @PluginSpringConfiguration(scanPluginPackage = false)
    static final class ScopedLifecyclePlugin implements ProxyPlugin {
        @Autowired
        private PluginContext pluginContext;

        @Autowired
        private PacketInterceptorRegistry interceptorRegistry;

        private boolean cleanupCalled;

        @OnLoad
        public void onLoad() {
            interceptorRegistry.register(PacketType.CHAT_SENT, context -> PacketDecision.forward());
            pluginContext.onRemove(() -> cleanupCalled = true);
        }

        @Override
        public PluginDescriptor descriptor() {
            return new PluginDescriptor("scoped-lifecycle", "Scoped Lifecycle", "1.0.0", List.of());
        }
    }
}
