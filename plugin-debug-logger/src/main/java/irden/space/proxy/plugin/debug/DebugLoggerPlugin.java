package irden.space.proxy.plugin.debug;

import irden.space.proxy.plugin_api.PluginContext;
import irden.space.proxy.plugin_api.PluginDescriptor;
import irden.space.proxy.plugin_api.ProxyPlugin;

import java.util.List;

public class DebugLoggerPlugin implements ProxyPlugin {

    @Override
    public PluginDescriptor descriptor() {
        return new PluginDescriptor(
                "debug-logger",
                "Debug Logger",
                "1.0.0",
                List.of()
        );
    }

    @Override
    public void onLoad(PluginContext context) {
        context.packetInterceptorRegistry().register(new DebugPacketInterceptor());
    }
}
