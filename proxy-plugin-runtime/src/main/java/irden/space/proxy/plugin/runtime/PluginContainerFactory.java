package irden.space.proxy.plugin.runtime;

import irden.space.proxy.plugin.api.PluginContext;
import irden.space.proxy.plugin.api.ProxyPlugin;

@FunctionalInterface
public interface PluginContainerFactory {

    PluginContainer create(ProxyPlugin plugin);

    default PluginContainer create(ProxyPlugin plugin, PluginContext pluginContext) {
        return create(plugin);
    }

    default PluginContainer create(PluginCandidate candidate, PluginContext pluginContext) {
        if (candidate.bootstrapInstance() == null) {
            throw new IllegalStateException(
                    "Plugin container factory cannot instantiate " + candidate.pluginClass().getName()
            );
        }
        return create(candidate.bootstrapInstance(), pluginContext);
    }

    static PluginContainerFactory unmanaged() {
        return plugin -> new PluginContainer() {
            @Override
            public ProxyPlugin plugin() {
                return plugin;
            }

            @Override
            public void close() {
                // Legacy plugins do not own a managed container.
            }
        };
    }
}
