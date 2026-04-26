package irden.space.proxy.plugin.runtime;


import irden.space.proxy.plugin.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class PluginManager implements PluginSessionLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(PluginManager.class);

    private final PluginLoader pluginLoader;
    private final PluginDependencyResolver dependencyResolver;
    private final PacketInterceptorRegistry interceptorRegistry;
    private final PluginContext pluginContext;
    private final PermissionBootstrapper permissionBootstrapper;

    private final List<ProxyPlugin> loadedPlugins = new ArrayList<>();

    public PluginManager(
            PluginLoader pluginLoader,
            PluginDependencyResolver dependencyResolver,
            PacketInterceptorRegistry interceptorRegistry,
            PluginContext pluginContext
    ) {
        this(
                pluginLoader,
                dependencyResolver,
                interceptorRegistry,
                pluginContext,
                new ClasspathPermissionBootstrapper()
        );
    }

    public PluginManager(
            PluginLoader pluginLoader,
            PluginDependencyResolver dependencyResolver,
            PacketInterceptorRegistry interceptorRegistry,
            PluginContext pluginContext,
            PermissionBootstrapper permissionBootstrapper
    ) {
        this.pluginLoader = pluginLoader;
        this.dependencyResolver = dependencyResolver;
        this.interceptorRegistry = interceptorRegistry;
        this.pluginContext = pluginContext;
        this.permissionBootstrapper = permissionBootstrapper;
    }

    public void loadAndStart() {
        permissionBootstrapper.bootstrap();

        List<ProxyPlugin> plugins = pluginLoader.loadPlugins();
        log.info("Discovered {} plugin(s)", plugins.size());
        for (ProxyPlugin plugin : plugins) {
            log.info("Discovered plugin: {}", describePlugin(plugin));
        }

        List<ProxyPlugin> ordered = dependencyResolver.resolveLoadOrder(plugins);
        log.info(
                "Resolved plugin load order: {}",
                ordered.stream()
                        .map(plugin -> plugin.descriptor().id())
                        .toList()
        );

        for (ProxyPlugin plugin : ordered) {
            log.info("Loading plugin {}", describePlugin(plugin));
            plugin.onLoad(pluginContext);
            loadedPlugins.add(plugin);
            log.info("Loaded plugin '{}'", plugin.descriptor().id());
        }

        for (ProxyPlugin plugin : loadedPlugins) {
            log.info("Starting plugin '{}'", plugin.descriptor().id());
            plugin.onStart();
            log.info("Started plugin '{}'", plugin.descriptor().id());
        }

        log.info(
                "Plugin runtime started with {} loaded plugin(s): {}",
                loadedPlugins.size(),
                loadedPlugins.stream()
                        .map(this::describePlugin)
                        .toList()
        );
    }

    public void stopAll() {
        log.info("Stopping {} plugin(s)", loadedPlugins.size());
        for (int i = loadedPlugins.size() - 1; i >= 0; i--) {
            ProxyPlugin plugin = loadedPlugins.get(i);
            log.info("Stopping plugin '{}'", plugin.descriptor().id());
            plugin.onStop();
            log.info("Stopped plugin '{}'", plugin.descriptor().id());
        }
    }

    @Override
    public void onConnectionSuccess(PluginSessionContext context) {
        for (ProxyPlugin plugin : loadedPlugins) {
            invokeSessionLifecycle(plugin, "OnConnectionSuccess", () -> plugin.onConnectionSuccess(context), context);
        }
    }

    @Override
    public void onDisconnecting(PluginSessionContext context) {
        for (int i = loadedPlugins.size() - 1; i >= 0; i--) {
            ProxyPlugin plugin = loadedPlugins.get(i);
            invokeSessionLifecycle(plugin, "OnDisconnecting", () -> plugin.onDisconnecting(context), context);
        }
    }

    @Override
    public void onDisconnected(PluginSessionContext context) {
        for (int i = loadedPlugins.size() - 1; i >= 0; i--) {
            ProxyPlugin plugin = loadedPlugins.get(i);
            invokeSessionLifecycle(plugin, "OnDisconnected", () -> plugin.onDisconnected(context), context);
        }

        pluginContext.findService(SessionPermissionService.class)
                .ifPresent(service -> service.clearPermissions(context.sessionId()));
    }

    public List<ProxyPlugin> getLoadedPlugins() {
        return List.copyOf(loadedPlugins);
    }

    public PacketInterceptorRegistry interceptorRegistry() {
        return interceptorRegistry;
    }

    private String describePlugin(ProxyPlugin plugin) {
        PluginDescriptor descriptor = plugin.descriptor();
        return "id='" + descriptor.id() + "', name='" + descriptor.name() + "', version='" + descriptor.version()
                + "', dependsOn=" + formatDependencies(descriptor.dependsOn());
    }

    private void invokeSessionLifecycle(
            ProxyPlugin plugin,
            String lifecycleName,
            Runnable invocation,
            PluginSessionContext context
    ) {
        try {
            invocation.run();
        } catch (RuntimeException e) {
            log.warn(
                    "Plugin '{}' failed during {} for session {}: {}",
                    plugin.descriptor().id(),
                    lifecycleName,
                    context.sessionId(),
                    e.getMessage(),
                    e
            );
        }
    }

    private String formatDependencies(List<String> dependencies) {
        return dependencies == null || dependencies.isEmpty() ? "[]" : dependencies.toString();
    }
}
