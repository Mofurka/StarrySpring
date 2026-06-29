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
    private final SessionPermissionService sessionPermissionService;

    private final CopyOnWriteArrayList<PluginContainer> loadedPlugins = new CopyOnWriteArrayList<>();
    private final Map<String, PluginRuntimeState> pluginStates = new HashMap<>();
    private final Map<String, PluginFailure> pluginFailures = new HashMap<>();
    private final Map<String, PluginCandidate> loadedCandidates = new HashMap<>();

    private static final String PHASE_LOAD = "LOAD";
    private static final String PHASE_START = "START";
    private static final String PHASE_STOP = "STOP";
    private static final String PHASE_RELOAD = "RELOAD";


    public PluginManager(
            PluginLoader pluginLoader,
            PluginDependencyResolver dependencyResolver,
            PacketInterceptorRegistry interceptorRegistry,
            PluginContext pluginContext,
            PluginContainerFactory containerFactory
    ) {
        this(pluginLoader, dependencyResolver, interceptorRegistry, pluginContext, containerFactory, null);
    }

    public PluginManager(
            PluginLoader pluginLoader,
            PluginDependencyResolver dependencyResolver,
            PacketInterceptorRegistry interceptorRegistry,
            PluginContext pluginContext,
            PluginContainerFactory containerFactory,
            SessionPermissionService sessionPermissionService
    ) {
        this.pluginLoader = pluginLoader;
        this.dependencyResolver = dependencyResolver;
        this.interceptorRegistry = interceptorRegistry;
        this.pluginContext = pluginContext;
        this.containerFactory = containerFactory;
        this.sessionPermissionService = sessionPermissionService;
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
        List<PluginCandidate> plugins = pluginLoader.loadPluginCandidates();
        log.info("Discovered {} plugin(s)", plugins.size());
        for (PluginCandidate plugin : plugins) {
            log.info("Discovered plugin: {}", describePlugin(plugin));
        }

        List<PluginCandidate> ordered = dependencyResolver.resolveCandidateLoadOrder(plugins);
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
        List<PluginCandidate> previousPluginCandidates = new ArrayList<>(pluginsToReload.size());
        for (int i = loadedPlugins.size() - 1; i >= 0; i--) {
            ProxyPlugin plugin = loadedPlugins.get(i).plugin();
            String loadedPluginId = plugin.descriptor().id();
            if (pluginsToReload.contains(loadedPluginId)) {
                PluginCandidate candidate = loadedCandidates.get(loadedPluginId);
                previousPluginCandidates.add(candidate != null
                        ? candidate
                        : new PluginCandidate(plugin.getClass().asSubclass(ProxyPlugin.class), plugin.descriptor()));
            }
        }

        pluginLoader.validateReloadPluginCandidates(previousPluginCandidates);
        List<String> stoppedPluginIds = stopPlugin(pluginId);
        pluginLoader.reloadPluginCandidates(previousPluginCandidates);
        try {
            return startPlugins(new LinkedHashSet<>(stoppedPluginIds));
        } catch (RuntimeException | Error e) {
            failState(pluginId, PHASE_RELOAD, e);
            rollbackReload(previousPluginCandidates);
            throw e;
        }
    }

    private void rollbackReload(List<PluginCandidate> previousPluginCandidates) {
        if (previousPluginCandidates.isEmpty()) {
            return;
        }

        // previousPluginCandidates is captured in reverse load order; reverse it back to load order.
        List<PluginCandidate> orderedPrevious = new ArrayList<>(previousPluginCandidates);
        Collections.reverse(orderedPrevious);
        try {
            startResolvedPlugins(orderedPrevious);
            log.info("Rolled back reload; restored the previous version of {} plugin(s)", orderedPrevious.size());
        } catch (RuntimeException | Error rollbackFailure) {
            log.error("Failed to roll back reload to the previous plugin version(s)", rollbackFailure);
        }
    }

    @Override
    public synchronized List<PluginRuntimeView> plugins() {
        Map<String, PluginRuntimeView> views = new LinkedHashMap<>();
        for (PluginCandidate plugin : pluginLoader.loadPluginCandidates()) {
            String pluginId = plugin.descriptor().id();
            views.put(pluginId, viewFor(plugin.descriptor(), PluginRuntimeState.STOPPED));
        }
        for (PluginContainer container : loadedPlugins) {
            ProxyPlugin plugin = container.plugin();
            views.put(plugin.descriptor().id(), viewFor(plugin.descriptor(), PluginRuntimeState.STARTED));
        }
        return List.copyOf(views.values());
    }

    private PluginRuntimeView viewFor(PluginDescriptor descriptor, PluginRuntimeState defaultState) {
        String pluginId = descriptor.id();
        PluginRuntimeState state = pluginStates.getOrDefault(pluginId, defaultState);
        return new PluginRuntimeView(descriptor, state, pluginFailures.get(pluginId));
    }

    private List<String> startPlugins(Set<String> requestedPluginIds) {
        Map<String, PluginCandidate> availablePlugins = new LinkedHashMap<>();
        for (PluginContainer container : loadedPlugins) {
            ProxyPlugin plugin = container.plugin();
            availablePlugins.put(
                    plugin.descriptor().id(),
                    new PluginCandidate(plugin.getClass().asSubclass(ProxyPlugin.class), plugin.descriptor())
            );
        }
        for (PluginCandidate plugin : pluginLoader.loadPluginCandidates()) {
            availablePlugins.putIfAbsent(plugin.descriptor().id(), plugin);
        }

        for (String requestedPluginId : requestedPluginIds) {
            if (!availablePlugins.containsKey(requestedPluginId)) {
                throw new NoSuchElementException("Plugin '" + requestedPluginId + "' is not available");
            }
        }

        List<PluginCandidate> ordered = dependencyResolver.resolveCandidateLoadOrder(List.copyOf(availablePlugins.values()));
        Set<String> requiredPluginIds = new LinkedHashSet<>();
        for (String requestedPluginId : requestedPluginIds) {
            requiredPluginIds.addAll(resolveDependencyClosure(requestedPluginId, availablePlugins));
        }
        List<PluginCandidate> pluginsToStart = ordered.stream()
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
            invokeSessionLifecycle(plugin, "OnConnectionSuccess", () -> container.onConnectionSuccess(context), context);
        }
    }

    @Override
    public void onDisconnecting(PluginSessionContext context) {
        for (int i = loadedPlugins.size() - 1; i >= 0; i--) {
            PluginContainer container = loadedPlugins.get(i);
            ProxyPlugin plugin = container.plugin();
            invokeSessionLifecycle(plugin, "OnDisconnecting", () -> plugin.onDisconnecting(context), context);
            invokeSessionLifecycle(plugin, "OnDisconnecting", () -> container.onDisconnecting(context), context);
        }
    }

    @Override
    public void onDisconnected(PluginSessionContext context) {
        for (int i = loadedPlugins.size() - 1; i >= 0; i--) {
            PluginContainer container = loadedPlugins.get(i);
            ProxyPlugin plugin = container.plugin();
            invokeSessionLifecycle(plugin, "OnDisconnected", () -> plugin.onDisconnected(context), context);
            invokeSessionLifecycle(plugin, "OnDisconnected", () -> container.onDisconnected(context), context);
        }

        if (sessionPermissionService != null) {
            sessionPermissionService.clearPermissions(context.sessionId());
        }
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

    private String describePlugin(PluginCandidate plugin) {
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
        return contextFor(plugin.descriptor().id());
    }

    private PluginContext contextFor(String pluginId) {
        if (pluginContext instanceof PluginContextManager contextManager) {
            return contextManager.forPlugin(pluginId);
        }
        return pluginContext;
    }

    private void removePluginRegistrations(ProxyPlugin plugin) {
        removePluginRegistrations(plugin.descriptor().id());
    }

    private void removePluginRegistrations(String pluginId) {
        if (pluginContext instanceof PluginContextManager contextManager) {
            contextManager.removePlugin(pluginId);
        }
    }

    private void setState(String pluginId, PluginRuntimeState state) {
        pluginStates.put(pluginId, state);
    }

    private void rememberFailure(String pluginId, String phase, Throwable error) {
        pluginFailures.put(pluginId, PluginFailure.of(pluginId, phase, error));
    }

    private void failState(String pluginId, String phase, Throwable error) {
        rememberFailure(pluginId, phase, error);
        setState(pluginId, PluginRuntimeState.FAILED);
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

    private Set<String> resolveDependencyClosure(String pluginId, Map<String, PluginCandidate> availablePlugins) {
        Set<String> result = new LinkedHashSet<>();
        collectDependencies(pluginId, availablePlugins, result);
        return result;
    }

    private void collectDependencies(
            String pluginId,
            Map<String, PluginCandidate> availablePlugins,
            Set<String> result
    ) {
        if (!result.add(pluginId)) {
            return;
        }

        PluginCandidate plugin = availablePlugins.get(pluginId);
        if (plugin == null) {
            throw new NoSuchElementException("Plugin '" + pluginId + "' is not available");
        }
        for (String dependencyId : plugin.descriptor().dependsOn()) {
            collectDependencies(dependencyId, availablePlugins, result);
        }
    }

    private List<String> startResolvedPlugins(List<PluginCandidate> plugins) {
        if (plugins.isEmpty()) {
            return List.of();
        }

        List<PluginContainer> startedContainers = new ArrayList<>(plugins.size());
        String failedPluginId = null;
        try {
            for (PluginCandidate candidate : plugins) {
                String pluginId = candidate.descriptor().id();
                log.info("Loading plugin {}", describePlugin(candidate));
                setState(pluginId, PluginRuntimeState.LOADING);
                PluginContext scopedContext = contextFor(pluginId);
                PluginContainer container = containerFactory.create(
                        candidate,
                        scopedContext,
                        dependencyContainers(candidate)
                );
                try {
                    ProxyPlugin plugin = container.plugin();
                    if (!candidate.descriptor().equals(plugin.descriptor())) {
                        throw new IllegalStateException(
                                "Spring-created plugin descriptor does not match discovered candidate: expected %s, got %s"
                                        .formatted(candidate.descriptor(), plugin.descriptor())
                        );
                    }
                    log.info("Registering permissions for plugin {}", describePlugin(plugin));
                    plugin.registerPluginPermissions(scopedContext);
                    container.registerPluginPermissions();
                    log.info("Registered permissions for plugin '{}'", plugin.descriptor().id());
                    container.registerAnnotatedBeans();
                    container.plugin().onLoad(scopedContext);
                    container.onLoad();
                    loadedPlugins.add(container);
                    loadedCandidates.put(pluginId, candidate);
                    startedContainers.add(container);
                    setState(pluginId, PluginRuntimeState.LOADED);
                    log.info("Loaded plugin '{}'", plugin.descriptor().id());
                } catch (RuntimeException | Error e) {
                    failedPluginId = pluginId;
                    failState(pluginId, PHASE_LOAD, e);
                    removePluginRegistrations(pluginId);
                    container.close();
                    throw e;
                }
            }

            for (PluginContainer container : startedContainers) {
                ProxyPlugin plugin = container.plugin();
                String pluginId = plugin.descriptor().id();
                log.info("Starting plugin '{}'", pluginId);
                try {
                    plugin.onStart();
                    container.onStart();
                    setState(pluginId, PluginRuntimeState.STARTED);
                    log.info("Started plugin '{}'", pluginId);
                } catch (RuntimeException | Error e) {
                    failedPluginId = pluginId;
                    failState(pluginId, PHASE_START, e);
                    throw e;
                }
            }
        } catch (RuntimeException | Error e) {
            for (int i = startedContainers.size() - 1; i >= 0; i--) {
                PluginContainer container = startedContainers.get(i);
                stopContainer(container);
                loadedPlugins.remove(container);
            }
            if (failedPluginId != null) {
                setState(failedPluginId, PluginRuntimeState.FAILED);
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
        String pluginId = plugin.descriptor().id();
        log.info("Stopping plugin '{}'", pluginId);
        setState(pluginId, PluginRuntimeState.STOPPING);
        try {
            plugin.onStop();
            container.onStop();
            log.info("Stopped plugin '{}'", pluginId);
        } catch (RuntimeException e) {
            log.warn("Plugin '{}' failed while stopping", pluginId, e);
            rememberFailure(pluginId, PHASE_STOP, e);
        } finally {
            try {
                removePluginRegistrations(plugin);
            } catch (RuntimeException e) {
                log.warn("Failed to remove registrations for plugin '{}'", pluginId, e);
                rememberFailure(pluginId, PHASE_STOP, e);
            } finally {
                try {
                    container.close();
                } catch (RuntimeException e) {
                    log.warn("Failed to close container for plugin '{}'", pluginId, e);
                    rememberFailure(pluginId, PHASE_STOP, e);
                } finally {
                    loadedCandidates.remove(pluginId);
                    setState(pluginId, PluginRuntimeState.STOPPED);
                }
            }
        }
    }

    private List<PluginContainer> dependencyContainers(PluginCandidate candidate) {
        List<PluginContainer> result = new ArrayList<>();
        for (String dependencyId : candidate.descriptor().dependsOn()) {
            PluginContainer dependency = findLoadedPlugin(dependencyId);
            if (dependency == null) {
                throw new NoSuchElementException(
                        "Plugin '%s' depends on unloaded plugin '%s'"
                                .formatted(candidate.descriptor().id(), dependencyId)
                );
            }
            result.add(dependency);
        }
        return List.copyOf(result);
    }
}
