package irden.space.proxy.plugin.runtime;



import irden.space.proxy.plugin.api.ProxyPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;


public class PluginLoader {

    public List<ProxyPlugin> loadPlugins() {
        ServiceLoader<ProxyPlugin> loader = ServiceLoader.load(ProxyPlugin.class);
        List<ProxyPlugin> plugins = new ArrayList<>();

        for (ProxyPlugin plugin : loader) {
            plugins.add(plugin);
        }

        return plugins;
    }
}
