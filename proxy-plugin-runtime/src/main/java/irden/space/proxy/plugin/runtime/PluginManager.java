package irden.space.proxy.plugin.runtime;


import irden.space.proxy.plugin.api.PacketInterceptorRegistry;
import irden.space.proxy.plugin.api.PluginContext;
import irden.space.proxy.plugin.api.ProxyPlugin;

import java.util.ArrayList;
import java.util.List;

public class PluginManager {

    private final PluginLoader pluginLoader;
    private final PluginDependencyResolver dependencyResolver;
    private final PacketInterceptorRegistry interceptorRegistry;
    private final PluginContext pluginContext;

    private final List<ProxyPlugin> loadedPlugins = new ArrayList<>();

    public PluginManager(
            PluginLoader pluginLoader,
            PluginDependencyResolver dependencyResolver,
            PacketInterceptorRegistry interceptorRegistry,
            PluginContext pluginContext
    ) {
        this.pluginLoader = pluginLoader;
        this.dependencyResolver = dependencyResolver;
        this.interceptorRegistry = interceptorRegistry;
        this.pluginContext = pluginContext;
    }

    public void loadAndStart() {
        List<ProxyPlugin> plugins = pluginLoader.loadPlugins();
        List<ProxyPlugin> ordered = dependencyResolver.resolveLoadOrder(plugins);

        for (ProxyPlugin plugin : ordered) {
            plugin.onLoad(pluginContext);
            loadedPlugins.add(plugin);
        }

        for (ProxyPlugin plugin : loadedPlugins) {
            plugin.onStart();
        }
    }

    public void stopAll() {
        for (int i = loadedPlugins.size() - 1; i >= 0; i--) {
            loadedPlugins.get(i).onStop();
        }
    }

    public List<ProxyPlugin> getLoadedPlugins() {
        return List.copyOf(loadedPlugins);
    }

    public PacketInterceptorRegistry interceptorRegistry() {
        return interceptorRegistry;
    }
}
