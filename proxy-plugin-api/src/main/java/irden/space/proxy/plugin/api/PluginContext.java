package irden.space.proxy.plugin.api;

public interface PluginContext {

    PacketInterceptorRegistry packetInterceptorRegistry();

    default void onRemove(Runnable cleanup) {
        // Optional lifecycle hook for managed plugin contexts.
    }
}
