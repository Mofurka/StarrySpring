package irden.space.proxy.plugin.runtime;

import irden.space.proxy.plugin.api.ProxyPlugin;

import java.util.Map;

public interface PluginContainer extends AutoCloseable {

    ProxyPlugin plugin();

    default <T> Map<String, T> beansOfType(Class<T> type) {
        return Map.of();
    }

    @Override
    void close();
}
