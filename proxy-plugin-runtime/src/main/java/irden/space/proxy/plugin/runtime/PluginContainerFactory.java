package irden.space.proxy.plugin.runtime;

import irden.space.proxy.plugin.api.ProxyPlugin;

@FunctionalInterface
public interface PluginContainerFactory {

    PluginContainer create(ProxyPlugin plugin);

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
