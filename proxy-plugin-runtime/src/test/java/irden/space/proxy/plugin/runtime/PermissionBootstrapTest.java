package irden.space.proxy.plugin.runtime;

import irden.space.proxy.plugin.api.PermissionRegistry;
import irden.space.proxy.plugin.api.PluginContext;
import irden.space.proxy.plugin.api.PluginDescriptor;
import irden.space.proxy.plugin.api.ProxyPlugin;
import irden.space.proxy.plugin.api.annotations.OnLoad;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermissionBootstrapTest {

    @Test
    void bootstrapsPermissionEnumsBeforePluginsLoad() {
        PermissionBootstrapProbe.onLoadObservedRegisteredPermission = false;
        PermissionBootstrapProbe.registrarObservedRegisteredPermission = false;

        var registry = new DefaultPacketInterceptorRegistry();
        PluginContext pluginContext = new DefaultPluginContext(registry);
        PluginLoader pluginLoader = new PluginLoader() {
            @Override
            public List<ProxyPlugin> loadPlugins() {
                return List.of(new BootstrapAwarePlugin());
            }
        };

        PluginManager pluginManager = new PluginManager(
                pluginLoader,
                new PluginDependencyResolver(),
                registry,
                pluginContext,
                new ClasspathPermissionBootstrapper("irden.space.proxy.plugin.runtime")
        );

        pluginManager.loadAndStart();

        assertTrue(PermissionBootstrapProbe.onLoadObservedRegisteredPermission);
        assertTrue(PermissionBootstrapProbe.registrarObservedRegisteredPermission);
        assertTrue(PermissionRegistry.entries().containsKey(PermissionBootstrapProbe.REGISTRAR_PERMISSION_NODE));
        assertTrue(PermissionRegistry.entries().containsKey(PermissionBootstrapProbe.ON_LOAD_PERMISSION_NODE));
        assertEquals(PermissionBootstrapProbe.ON_LOAD_PERMISSION_NODE, PermissionBootstrapTestPermissions.ON_LOAD.permission().name());
    }

    public static final class BootstrapAwarePlugin implements ProxyPlugin {

        @Override
        public PluginDescriptor descriptor() {
            return new PluginDescriptor("bootstrap-aware-plugin", "bootstrap-aware-plugin", "1.0.0", List.of());
        }

        @OnLoad
        void handleLoad(PluginContext context) {
            PermissionBootstrapProbe.onLoadObservedRegisteredPermission =
                    PermissionRegistry.entries().containsKey(PermissionBootstrapProbe.ON_LOAD_PERMISSION_NODE)
                            && PermissionBootstrapTestPermissions.ON_LOAD.permission().name().equals(PermissionBootstrapProbe.ON_LOAD_PERMISSION_NODE);
        }
    }
}
