package irden.space.boot;

import irden.space.proxy.plugin.api.*;
import irden.space.proxy.plugin.runtime.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.nio.file.Path;

@Configuration
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
    public PluginLoader pluginLoader(@Value("${starry.plugins.directory:runtime-plugins}") String pluginsDirectory) {
        return new Pf4jPluginLoader(Path.of(pluginsDirectory));
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
            PluginContext pluginContext,
            PluginContainerFactory pluginContainerFactory
    ) {
        PluginManager manager = new PluginManager(
                pluginLoader,
                pluginDependencyResolver,
                packetInterceptorRegistry,
                pluginContext,
                pluginContainerFactory
        );
        pluginContext.publishService(PluginRuntimeService.class, manager);
        return manager;
    }

    @Bean
    public PluginContainerFactory pluginContainerFactory(ApplicationContext applicationContext) {
        return new SpringPluginContainerFactory(applicationContext);
    }

    @Bean
    public PacketInterceptionService packetInterceptionChain(PacketInterceptorRegistry packetInterceptorRegistry) {
        return new PacketInterceptionChain(packetInterceptorRegistry);
    }
}
