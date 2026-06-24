package irden.space.proxy.plugin.api;

import org.pf4j.ExtensionPoint;

public interface ProxyPlugin extends ExtensionPoint {

    default PluginDescriptor descriptor() {
        return ProxyPluginSupport.descriptor(this);
    }

    default void registerPluginPermissions(PluginContext context) {
        // Optional runtime phase.
    }

    default void onLoad(PluginContext context) {
        // Optional runtime phase.
    }

    default void onStart() {
        // Optional runtime phase.
    }


    // This method is called when a new connection is established. Plugins can override this to perform actions when a client connects. By default, it does nothing.
    default void onConnectionSuccess(PluginSessionContext context) {
        // Optional runtime phase.
    }


    // For plugins that want to handle disconnection events, they can override these methods. By default, they do nothing.
    default void onDisconnecting(PluginSessionContext context) {
        // Optional runtime phase.
    }


    // This method is called after the connection has been fully closed. Plugins can override this to perform cleanup tasks. By default, it does nothing.
    default void onDisconnected(PluginSessionContext context) {
        // Optional runtime phase.
    }

    default void onStop() {
        // Optional runtime phase.
    }
}
