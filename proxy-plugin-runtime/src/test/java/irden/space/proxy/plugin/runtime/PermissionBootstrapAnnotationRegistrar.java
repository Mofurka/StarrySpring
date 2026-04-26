package irden.space.proxy.plugin.runtime;

import irden.space.proxy.plugin.api.PermissionRegistry;
import irden.space.proxy.plugin.api.PluginAnnotationRegistrar;
import irden.space.proxy.plugin.api.PluginContext;
import irden.space.proxy.plugin.api.ProxyPlugin;

public final class PermissionBootstrapAnnotationRegistrar implements PluginAnnotationRegistrar {

    @Override
    public boolean supports(Class<?> pluginType) {
        return PermissionBootstrapProbe.REGISTRAR_AWARE_PLUGIN_SIMPLE_NAME.equals(pluginType.getSimpleName());
    }

    @Override
    public void register(ProxyPlugin plugin, PluginContext context) {
        PermissionBootstrapProbe.registrarObservedRegisteredPermission =
                PermissionRegistry.entries().containsKey(PermissionBootstrapProbe.REGISTRAR_PERMISSION_NODE);
    }
}
