package irden.space.proxy.plugin.runtime;



import irden.space.proxy.plugin.api.ProxyPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;


public class PluginLoader implements AutoCloseable {

    public List<ProxyPlugin> loadPlugins() {
        ServiceLoader<ProxyPlugin> loader = ServiceLoader.load(ProxyPlugin.class);
        List<ProxyPlugin> plugins = new ArrayList<>();

        for (ProxyPlugin plugin : loader) {
            plugins.add(plugin);
        }

        return plugins;
    }

    public void reloadPlugins(List<ProxyPlugin> plugins) {
        // Classpath plugins cannot reload their application class loader.
    }

    public void validateReloadPlugins(List<ProxyPlugin> plugins) {
        // Classpath plugins only recreate their managed containers.
    }

    @Override
    public void close() {
        // Classpath plugins do not own a dedicated class loader.
    }
}
