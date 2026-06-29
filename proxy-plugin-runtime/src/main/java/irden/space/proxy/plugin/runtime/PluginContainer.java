package irden.space.proxy.plugin.runtime;

import irden.space.proxy.plugin.api.ProxyPlugin;

import java.util.Map;

public interface PluginContainer extends AutoCloseable {

    ProxyPlugin plugin();

    default <T> Map<String, T> beansOfType(Class<T> type) {
        return Map.of();
    }

    default void registerAnnotatedBeans() {
    }

    default void registerPluginPermissions() {
    }

    default void onLoad() {
    }

    default void onStart() {
    }

    default void onConnectionSuccess(irden.space.proxy.plugin.api.PluginSessionContext context) {
    }

    default void onDisconnecting(irden.space.proxy.plugin.api.PluginSessionContext context) {
    }

    default void onDisconnected(irden.space.proxy.plugin.api.PluginSessionContext context) {
    }

    default void onStop() {
    }

    @Override
    void close();
}
