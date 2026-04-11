package irden.space.proxy.plugin_runtime;


import irden.space.proxy.plugin_api.PacketInterceptorRegistry;
import irden.space.proxy.plugin_api.PluginContext;

public class DefaultPluginContext implements PluginContext {

    private final PacketInterceptorRegistry packetInterceptorRegistry;

    public DefaultPluginContext(PacketInterceptorRegistry packetInterceptorRegistry) {
        this.packetInterceptorRegistry = packetInterceptorRegistry;
    }

    @Override
    public PacketInterceptorRegistry packetInterceptorRegistry() {
        return packetInterceptorRegistry;
    }
}
