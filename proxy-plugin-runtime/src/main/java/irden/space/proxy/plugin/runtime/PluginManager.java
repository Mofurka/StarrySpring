package irden.space.proxy.plugin.runtime;


import irden.space.proxy.plugin.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class PluginManager implements PluginSessionLifecycleService, PluginRuntimeService {

    private static final Logger log = LoggerFactory.getLogger(PluginManager.class);

    private final PluginLoader pluginLoader;
    private final PluginDependencyResolver dependencyResolver;
    private final PacketInterceptorRegistry interceptorRegistry;
    private final PluginContext pluginContext;
    private final PluginContainerFactory containerFactory;

    private final CopyOnWriteArrayList<PluginContainer> loadedPlugins = new CopyOnWriteArrayList<>();


    public PluginManager(
            PluginLoader pluginLoader,
            PluginDependencyResolver dependencyResolver,
            PacketInterceptorRegistry interceptorRegistry,
            PluginContext pluginContext
    ) {
        this(pluginLoader, dependencyResolver, interceptorRegistry, pluginContext, PluginContainerFactory.unmanaged());
    }

    public PluginManager(
            PluginLoader pluginLoader,
            PluginDependencyResolver dependencyResolver,
            PacketInterceptorRegistry interceptorRegistry,
            PluginContext pluginContext,
            PluginContainerFactory containerFactory
    ) {
        this.pluginLoader = pluginLoader;
        this.dependencyResolver = dependencyResolver;
        this.interceptorRegistry = interceptorRegistry;
        this.pluginContext = pluginContext;
        this.containerFactory = containerFactory;
    }

    public synchronized void loadAndStart() {
        try {
            doLoadAndStart();
        } catch (RuntimeException | Error e) {
            stopAll();
            throw e;
        }
    }

    private void doLoadAndStart() {
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

        startResolvedPlugins(ordered);

        log.info(
                "Plugin runtime started with {} loaded plugin(s)",
                loadedPlugins.size()

        );
        loadedPlugins.stream()
                .map(PluginContainer::plugin)
                .map(this::describePlugin)
                .forEach(pluginInfo -> log.info("Loaded plugin: {}", pluginInfo));
    }

    public synchronized void stopAll() {
        log.info("Stopping {} plugin(s)", loadedPlugins.size());
        try {
            for (int i = loadedPlugins.size() - 1; i >= 0; i--) {
                stopContainer(loadedPlugins.get(i));
            }
        } finally {
            loadedPlugins.clear();
            pluginLoader.close();
        }
    }

    public synchronized List<String> stopPlugin(String pluginId) {
        if (pluginId == null || pluginId.isBlank()) {
            throw new IllegalArgumentException("Plugin id must not be blank");
        }
        if (findLoadedPlugin(pluginId) == null) {
            throw new NoSuchElementException("Plugin '" + pluginId + "' is not loaded");
        }

        Set<String> pluginsToStop = resolveDependentClosure(pluginId);
        List<String> stoppedPluginIds = new ArrayList<>(pluginsToStop.size());

        for (int i = loadedPlugins.size() - 1; i >= 0; i--) {
            PluginContainer container = loadedPlugins.get(i);
            String loadedPluginId = container.plugin().descriptor().id();
            if (!pluginsToStop.contains(loadedPluginId)) {
                continue;
            }

            stopContainer(container);
            loadedPlugins.remove(container);
            stoppedPluginIds.add(loadedPluginId);
        }

        return List.copyOf(stoppedPluginIds);
    }

    public synchronized List<String> startPlugin(String pluginId) {
        if (pluginId == null || pluginId.isBlank()) {
            throw new IllegalArgumentException("Plugin id must not be blank");
        }
        if (findLoadedPlugin(pluginId) != null) {
            return List.of();
        }

        return startPlugins(Set.of(pluginId));
    }

    public synchronized List<String> reloadPlugin(String pluginId) {
        Set<String> pluginsToReload = resolveDependentClosure(pluginId);
        List<ProxyPlugin> previousPluginInstances = new ArrayList<>(pluginsToReload.size());
        for (int i = loadedPlugins.size() - 1; i >= 0; i--) {
            ProxyPlugin plugin = loadedPlugins.get(i).plugin();
            if (pluginsToReload.contains(plugin.descriptor().id())) {
                previousPluginInstances.add(plugin);
            }
        }

        pluginLoader.validateReloadPlugins(previousPluginInstances);
        List<String> stoppedPluginIds = stopPlugin(pluginId);
        pluginLoader.reloadPlugins(previousPluginInstances);
        return startPlugins(new LinkedHashSet<>(stoppedPluginIds));
    }

    @Override
    public synchronized List<PluginRuntimeView> plugins() {
        Map<String, PluginRuntimeView> views = new LinkedHashMap<>();
        for (ProxyPlugin plugin : pluginLoader.loadPlugins()) {
            views.put(
                    plugin.descriptor().id(),
                    new PluginRuntimeView(plugin.descriptor(), PluginRuntimeState.STOPPED)
            );
        }
        for (PluginContainer container : loadedPlugins) {
            ProxyPlugin plugin = container.plugin();
            views.put(
                    plugin.descriptor().id(),
                    new PluginRuntimeView(plugin.descriptor(), PluginRuntimeState.LOADED)
            );
        }
        return List.copyOf(views.values());
    }

    private List<String> startPlugins(Set<String> requestedPluginIds) {
        Map<String, ProxyPlugin> availablePlugins = new LinkedHashMap<>();
        for (PluginContainer container : loadedPlugins) {
            ProxyPlugin plugin = container.plugin();
            availablePlugins.put(plugin.descriptor().id(), plugin);
        }
        for (ProxyPlugin plugin : pluginLoader.loadPlugins()) {
            availablePlugins.putIfAbsent(plugin.descriptor().id(), plugin);
        }

        for (String requestedPluginId : requestedPluginIds) {
            if (!availablePlugins.containsKey(requestedPluginId)) {
                throw new NoSuchElementException("Plugin '" + requestedPluginId + "' is not available");
            }
        }

        List<ProxyPlugin> ordered = dependencyResolver.resolveLoadOrder(List.copyOf(availablePlugins.values()));
        Set<String> requiredPluginIds = new LinkedHashSet<>();
        for (String requestedPluginId : requestedPluginIds) {
            requiredPluginIds.addAll(resolveDependencyClosure(requestedPluginId, availablePlugins));
        }
        List<ProxyPlugin> pluginsToStart = ordered.stream()
                .filter(plugin -> requiredPluginIds.contains(plugin.descriptor().id()))
                .filter(plugin -> findLoadedPlugin(plugin.descriptor().id()) == null)
                .toList();

        return startResolvedPlugins(pluginsToStart);
    }

    @Override
    public void onConnectionSuccess(PluginSessionContext context) {
        for (PluginContainer container : loadedPlugins) {
            ProxyPlugin plugin = container.plugin();
            invokeSessionLifecycle(plugin, "OnConnectionSuccess", () -> plugin.onConnectionSuccess(context), context);
        }
    }

    @Override
    public void onDisconnecting(PluginSessionContext context) {
        for (int i = loadedPlugins.size() - 1; i >= 0; i--) {
            ProxyPlugin plugin = loadedPlugins.get(i).plugin();
            invokeSessionLifecycle(plugin, "OnDisconnecting", () -> plugin.onDisconnecting(context), context);
        }
    }

    @Override
    public void onDisconnected(PluginSessionContext context) {
        for (int i = loadedPlugins.size() - 1; i >= 0; i--) {
            ProxyPlugin plugin = loadedPlugins.get(i).plugin();
            invokeSessionLifecycle(plugin, "OnDisconnected", () -> plugin.onDisconnected(context), context);
        }

        pluginContext.findService(SessionPermissionService.class)
                .ifPresent(service -> service.clearPermissions(context.sessionId()));
    }

    public List<ProxyPlugin> getLoadedPlugins() {
        return loadedPlugins.stream()
                .map(PluginContainer::plugin)
                .toList();
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

    private PluginContext contextFor(ProxyPlugin plugin) {
        if (pluginContext instanceof PluginContextManager contextManager) {
            return contextManager.forPlugin(plugin.descriptor().id());
        }
        return pluginContext;
    }

    private void removePluginRegistrations(ProxyPlugin plugin) {
        if (pluginContext instanceof PluginContextManager contextManager) {
            contextManager.removePlugin(plugin.descriptor().id());
        }
    }

    private PluginContainer findLoadedPlugin(String pluginId) {
        for (PluginContainer container : loadedPlugins) {
            if (pluginId.equals(container.plugin().descriptor().id())) {
                return container;
            }
        }
        return null;
    }

    private Set<String> resolveDependentClosure(String pluginId) {
        Set<String> result = new LinkedHashSet<>();
        result.add(pluginId);

        boolean changed;
        do {
            changed = false;
            for (PluginContainer container : loadedPlugins) {
                ProxyPlugin plugin = container.plugin();
                if (result.contains(plugin.descriptor().id())) {
                    continue;
                }
                if (plugin.descriptor().dependsOn().stream().anyMatch(result::contains)) {
                    changed |= result.add(plugin.descriptor().id());
                }
            }
        } while (changed);

        return result;
    }

    private Set<String> resolveDependencyClosure(String pluginId, Map<String, ProxyPlugin> availablePlugins) {
        Set<String> result = new LinkedHashSet<>();
        collectDependencies(pluginId, availablePlugins, result);
        return result;
    }

    private void collectDependencies(
            String pluginId,
            Map<String, ProxyPlugin> availablePlugins,
            Set<String> result
    ) {
        if (!result.add(pluginId)) {
            return;
        }

        ProxyPlugin plugin = availablePlugins.get(pluginId);
        if (plugin == null) {
            throw new NoSuchElementException("Plugin '" + pluginId + "' is not available");
        }
        for (String dependencyId : plugin.descriptor().dependsOn()) {
            collectDependencies(dependencyId, availablePlugins, result);
        }
    }

    private List<String> startResolvedPlugins(List<ProxyPlugin> plugins) {
        if (plugins.isEmpty()) {
            return List.of();
        }

        List<PluginContainer> startedContainers = new ArrayList<>(plugins.size());
        try {
            for (ProxyPlugin plugin : plugins) {
                log.info("Registering permissions for plugin {}", describePlugin(plugin));
                plugin.registerPluginPermissions(contextFor(plugin));
                log.info("Registered permissions for plugin '{}'", plugin.descriptor().id());
            }

            for (ProxyPlugin plugin : plugins) {
                log.info("Loading plugin {}", describePlugin(plugin));
                PluginContainer container = containerFactory.create(plugin);
                try {
                    container.plugin().onLoad(contextFor(plugin));
                    loadedPlugins.add(container);
                    startedContainers.add(container);
                    log.info("Loaded plugin '{}'", plugin.descriptor().id());
                } catch (RuntimeException | Error e) {
                    removePluginRegistrations(plugin);
                    container.close();
                    throw e;
                }
            }

            for (PluginContainer container : startedContainers) {
                ProxyPlugin plugin = container.plugin();
                log.info("Starting plugin '{}'", plugin.descriptor().id());
                plugin.onStart();
                log.info("Started plugin '{}'", plugin.descriptor().id());
            }
        } catch (RuntimeException | Error e) {
            for (int i = startedContainers.size() - 1; i >= 0; i--) {
                PluginContainer container = startedContainers.get(i);
                stopContainer(container);
                loadedPlugins.remove(container);
            }
            throw e;
        }

        return startedContainers.stream()
                .map(PluginContainer::plugin)
                .map(plugin -> plugin.descriptor().id())
                .toList();
    }

    private void stopContainer(PluginContainer container) {
        ProxyPlugin plugin = container.plugin();
        log.info("Stopping plugin '{}'", plugin.descriptor().id());
        try {
            plugin.onStop();
            log.info("Stopped plugin '{}'", plugin.descriptor().id());
        } catch (RuntimeException e) {
            log.warn("Plugin '{}' failed while stopping", plugin.descriptor().id(), e);
        } finally {
            try {
                removePluginRegistrations(plugin);
            } catch (RuntimeException e) {
                log.warn("Failed to remove registrations for plugin '{}'", plugin.descriptor().id(), e);
            } finally {
                try {
                    container.close();
                } catch (RuntimeException e) {
                    log.warn("Failed to close container for plugin '{}'", plugin.descriptor().id(), e);
                }
            }
        }
    }
}
