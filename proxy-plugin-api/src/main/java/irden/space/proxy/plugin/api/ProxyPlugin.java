package irden.space.proxy.plugin.api;


public interface ProxyPlugin {

    PluginDescriptor descriptor();

    default void onLoad(PluginContext context) {
    }

    default void onStart() {
    }

    default void onStop() {
    }
}
