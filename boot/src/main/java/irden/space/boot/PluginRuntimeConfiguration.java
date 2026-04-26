package irden.space.boot;

import irden.space.proxy.plugin.api.PacketInterceptionService;
import irden.space.proxy.plugin.api.PacketInterceptorRegistry;
import irden.space.proxy.plugin.api.PluginContext;
import irden.space.proxy.plugin.api.SessionPermissionService;
import irden.space.proxy.plugin.runtime.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

import javax.sql.DataSource;

@Configuration
@ImportRuntimeHints(PluginNativeRuntimeHints.class)
public class PluginRuntimeConfiguration {

    @Bean
    public PacketInterceptorRegistry packetInterceptorRegistry() {
        return new DefaultPacketInterceptorRegistry();
    }

    @Bean
    public PluginContext pluginContext(
            PacketInterceptorRegistry packetInterceptorRegistry,
            DataSource dataSource,
            SessionPermissionService sessionPermissionService
    ) {
        DefaultPluginContext context = new DefaultPluginContext(packetInterceptorRegistry);
        context.publishService(DataSource.class, dataSource);
        context.publishService(SessionPermissionService.class, sessionPermissionService);
        return context;
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
    public PermissionBootstrapper permissionBootstrapper() {
        return new ClasspathPermissionBootstrapper();
    }

    @Bean
    public PluginManager pluginManager(
            PluginLoader pluginLoader,
            PluginDependencyResolver pluginDependencyResolver,
            PacketInterceptorRegistry packetInterceptorRegistry,
            PluginContext pluginContext,
            PermissionBootstrapper permissionBootstrapper
    ) {
        return new PluginManager(
                pluginLoader,
                pluginDependencyResolver,
                packetInterceptorRegistry,
                pluginContext,
                permissionBootstrapper
        );
    }

    @Bean
    public PacketInterceptionService packetInterceptionChain(PacketInterceptorRegistry packetInterceptorRegistry) {
        return new PacketInterceptionChain(packetInterceptorRegistry);
    }
}