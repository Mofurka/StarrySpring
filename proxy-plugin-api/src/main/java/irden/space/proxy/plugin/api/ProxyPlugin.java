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


    // This method is called when a new connection is established. Plugins can override this to perform actions when a client connects. By default, it does nothing.
    default void onConnectionSuccess(PluginSessionContext context) {
        ProxyPluginSupport.onConnectionSuccess(this, context);
    }


    // For plugins that want to handle disconnection events, they can override these methods. By default, they do nothing.
    default void onDisconnecting(PluginSessionContext context) {
        ProxyPluginSupport.onDisconnecting(this, context);
    }


    // This method is called after the connection has been fully closed. Plugins can override this to perform cleanup tasks. By default, it does nothing.
    default void onDisconnected(PluginSessionContext context) {
        ProxyPluginSupport.onDisconnected(this, context);
    }

    default void onStop() {
        ProxyPluginSupport.onStop(this);
    }
}
