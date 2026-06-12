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

    public List<PluginCandidate> loadPluginCandidates() {
        return loadPlugins().stream()
                .map(PluginCandidate::fromInstance)
                .toList();
    }

    public void reloadPlugins(List<ProxyPlugin> plugins) {
        // Classpath plugins cannot reload their application class loader.
    }

    public void reloadPluginCandidates(List<PluginCandidate> plugins) {
        reloadPlugins(plugins.stream()
                .map(PluginCandidate::bootstrapInstance)
                .filter(java.util.Objects::nonNull)
                .toList());
    }

    public void validateReloadPlugins(List<ProxyPlugin> plugins) {
        // Classpath plugins only recreate their managed containers.
    }

    public void validateReloadPluginCandidates(List<PluginCandidate> plugins) {
        validateReloadPlugins(plugins.stream()
                .map(PluginCandidate::bootstrapInstance)
                .filter(java.util.Objects::nonNull)
                .toList());
    }

    @Override
    public void close() {
        // Classpath plugins do not own a dedicated class loader.
    }
}
