package irden.space.proxy.plugin.api;


public interface ProxyPlugin {

    default PluginDescriptor descriptor() {
        return ProxyPluginSupport.descriptor(this);
    }

    default void onLoad(PluginContext context) {
        ProxyPluginSupport.onLoad(this, context);
    }

    default void onStart() {
        ProxyPluginSupport.onStart(this);
    }

    default void onStop() {
        ProxyPluginSupport.onStop(this);
    }
}
