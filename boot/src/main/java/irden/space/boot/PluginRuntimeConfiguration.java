package irden.space.boot;

import irden.space.proxy.plugin_api.PacketInterceptorRegistry;
import irden.space.proxy.plugin_api.PacketInterceptionService;
import irden.space.proxy.plugin_api.PluginContext;
import irden.space.proxy.plugin_runtime.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PluginRuntimeConfiguration {

    @Bean
    public PacketInterceptorRegistry packetInterceptorRegistry() {
        return new DefaultPacketInterceptorRegistry();
    }

    @Bean
    public PluginContext pluginContext(PacketInterceptorRegistry packetInterceptorRegistry) {
        return new DefaultPluginContext(packetInterceptorRegistry);
    }

    @Bean
    public PluginLoader pluginLoader() {
        return new PluginLoader();
    }

    @Bean
    public PluginDependencyResolver pluginDependencyResolver() {
        return new PluginDependencyResolver();
    }

    @Bean
    public PluginManager pluginManager(
            PluginLoader pluginLoader,
            PluginDependencyResolver pluginDependencyResolver,
            PacketInterceptorRegistry packetInterceptorRegistry,
            PluginContext pluginContext
    ) {
        return new PluginManager(
                pluginLoader,
                pluginDependencyResolver,
                packetInterceptorRegistry,
                pluginContext
        );
    }

    @Bean
    public PacketInterceptionService packetInterceptionChain(PacketInterceptorRegistry packetInterceptorRegistry) {
        return new PacketInterceptionChain(packetInterceptorRegistry);
    }
}
