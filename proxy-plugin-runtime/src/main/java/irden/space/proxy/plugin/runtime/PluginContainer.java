package irden.space.proxy.plugin.runtime;

import irden.space.proxy.plugin.api.ProxyPlugin;

public interface PluginContainer extends AutoCloseable {

    ProxyPlugin plugin();

    @Override
    void close();
}
