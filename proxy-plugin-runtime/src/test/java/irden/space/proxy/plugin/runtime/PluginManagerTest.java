package irden.space.proxy.plugin.runtime;

import irden.space.proxy.plugin.api.*;
import irden.space.proxy.protocol.packet.PacketType;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PluginManagerTest {

    @Test
    void loadAndStartLogsAgainstResolvedDependencyOrder() {
        List<String> lifecycleEvents = new ArrayList<>();

        TestPlugin corePlugin = new TestPlugin("core", List.of(), lifecycleEvents);
        TestPlugin featurePlugin = new TestPlugin("feature", List.of("core"), lifecycleEvents);

        PluginLoader pluginLoader = new PluginLoader() {
            @Override
            public List<PluginCandidate> loadPluginCandidates() {
                return candidates(featurePlugin, corePlugin);
            }
        };

        PacketInterceptorRegistry registry = new PacketInterceptorRegistry() {
            private final List<PacketInterceptor> interceptors = new ArrayList<>();

            @Override
            public void register(PacketInterceptor interceptor) {
                interceptors.add(interceptor);
            }

            @Override
            public List<PacketInterceptor> getAll() {
                return List.copyOf(interceptors);
            }
        };

        PluginContext pluginContext = new DefaultPluginContext(registry);

        PluginManager manager = new PluginManager(
                pluginLoader,
                new PluginDependencyResolver(),
                registry,
                pluginContext,
                testContainers()
        );

        manager.loadAndStart();

        assertEquals(
                List.of("load:core", "load:feature", "start:core", "start:feature"),
                lifecycleEvents
        );
        assertEquals(List.of(corePlugin, featurePlugin), manager.getLoadedPlugins());

        manager.stopAll();

        assertEquals(
                List.of(
                        "load:core",
                        "load:feature",
                        "start:core",
                        "start:feature",
                        "stop:feature",
                        "stop:core"
                ),
                lifecycleEvents
        );
    }

    @Test
    void dispatchesSessionLifecycleEventsAcrossLoadedPlugins() {
        List<String> lifecycleEvents = new ArrayList<>();

        SessionLifecyclePlugin corePlugin = new SessionLifecyclePlugin("core", List.of(), lifecycleEvents);
        SessionLifecyclePlugin featurePlugin = new SessionLifecyclePlugin("feature", List.of("core"), lifecycleEvents);

        PluginLoader pluginLoader = new PluginLoader() {
            @Override
            public List<PluginCandidate> loadPluginCandidates() {
                return candidates(featurePlugin, corePlugin);
            }
        };
        PacketInterceptorRegistry registry = new PacketInterceptorRegistry() {
            @Override
            public void register(PacketInterceptor interceptor) {
                // no-op for this test
            }

            @Override
            public List<PacketInterceptor> getAll() {
                return List.of();
            }
        };
        PluginContext pluginContext = new DefaultPluginContext(registry);

        PluginManager manager = new PluginManager(
                pluginLoader,
                new PluginDependencyResolver(),
                registry,
                pluginContext,
                testContainers()
        );

        manager.loadAndStart();

        PluginSessionContext sessionContext = new DefaultPluginSessionContext("session-42", "127.0.0.1", false, false);
        manager.onConnectionSuccess(sessionContext);
        manager.onDisconnecting(sessionContext);
        manager.onDisconnected(sessionContext);

        assertEquals(
                List.of(
                        "load:core",
                        "load:feature",
                        "start:core",
                        "start:feature",
                        "connected:core:session-42",
                        "connected:feature:session-42",
                        "disconnecting:feature:session-42",
                        "disconnecting:core:session-42",
                        "disconnected:feature:session-42",
                        "disconnected:core:session-42"
                ),
                lifecycleEvents
        );
    }

    @Test
    void clearsSessionPermissionsAfterDisconnectedCallbacksComplete() {
        List<String> lifecycleEvents = new ArrayList<>();
        RecordingSessionPermissionService permissionService = new RecordingSessionPermissionService();
        permissionService.updatePermissions("session-99", permissionId -> permissionId == 7);

        SessionLifecyclePlugin plugin = new SessionLifecyclePlugin("core", List.of(), lifecycleEvents) {
            @Override
            public void onDisconnected(PluginSessionContext context) {
                lifecycleEvents.add("permission:" + context.permissions().has(7));
            }
        };

        PluginLoader pluginLoader = new PluginLoader() {
            @Override
            public List<PluginCandidate> loadPluginCandidates() {
                return candidates(plugin);
            }
        };
        PacketInterceptorRegistry registry = new PacketInterceptorRegistry() {
            @Override
            public void register(PacketInterceptor interceptor) {
                // no-op for this test
            }

            @Override
            public List<PacketInterceptor> getAll() {
                return List.of();
            }
        };
        DefaultPluginContext pluginContext = new DefaultPluginContext(registry);
        pluginContext.publishService(SessionPermissionService.class, permissionService);

        PluginManager manager = new PluginManager(
                pluginLoader,
                new PluginDependencyResolver(),
                registry,
                pluginContext,
                testContainers()
        );

        manager.loadAndStart();

        PluginSessionContext sessionContext = new DefaultPluginSessionContext(
                "session-99",
                "127.0.0.1",
                false,
                false,
                1,
                null,
                permissionId -> permissionService.permissions("session-99").has(permissionId)
        );

        assertTrue(sessionContext.permissions().has(7));

        manager.onDisconnected(sessionContext);

        assertEquals(List.of("load:core", "start:core", "permission:true"), lifecycleEvents);
        assertFalse(permissionService.permissions("session-99").has(7));
    }

    @Test
    void closesPluginContainersAndLoaderWhenStopped() {
        List<String> lifecycleEvents = new ArrayList<>();
        TestPlugin plugin = new TestPlugin("core", List.of(), lifecycleEvents);
        boolean[] loaderClosed = {false};

        PluginLoader pluginLoader = new PluginLoader() {
            @Override
            public List<PluginCandidate> loadPluginCandidates() {
                return candidates(plugin);
            }

            @Override
            public void close() {
                loaderClosed[0] = true;
            }
        };

        PacketInterceptorRegistry registry = new PacketInterceptorRegistry() {
            @Override
            public void register(PacketInterceptor interceptor) {
            }

            @Override
            public List<PacketInterceptor> getAll() {
                return List.of();
            }
        };

        List<String> closedContainers = new ArrayList<>();
        PluginContainerFactory containerFactory = (candidate, context) -> new PluginContainer() {
            @Override
            public ProxyPlugin plugin() {
                return pluginFor(candidate);
            }

            @Override
            public void close() {
                closedContainers.add(candidate.descriptor().id());
            }
        };

        PluginManager manager = new PluginManager(
                pluginLoader,
                new PluginDependencyResolver(),
                registry,
                new DefaultPluginContext(registry),
                containerFactory
        );

        manager.loadAndStart();
        manager.stopAll();

        assertEquals(List.of("core"), closedContainers);
        assertTrue(loaderClosed[0]);
        assertTrue(manager.getLoadedPlugins().isEmpty());
    }

    @Test
    void removesPluginOwnedServicesAndInterceptorsWhenStopped() {
        DefaultPacketInterceptorRegistry registry = new DefaultPacketInterceptorRegistry();
        DefaultPluginContext pluginContext = new DefaultPluginContext(registry);
        Runnable applicationService = () -> {
        };
        pluginContext.publishService(Runnable.class, applicationService);

        ProxyPlugin plugin = new TestPlugin("core", List.of(), new ArrayList<>()) {
            @Override
            public void onLoad(PluginContext context) {
                context.publishService(Comparable.class, new ComparableService());
                context.packetInterceptorRegistry().register(PacketType.CHAT_SENT, packetContext -> PacketDecision.forward());
            }
        };

        PluginLoader pluginLoader = new PluginLoader() {
            @Override
            public List<PluginCandidate> loadPluginCandidates() {
                return candidates(plugin);
            }
        };

        PluginManager manager = new PluginManager(
                pluginLoader,
                new PluginDependencyResolver(),
                registry,
                pluginContext,
                testContainers()
        );

        manager.loadAndStart();

        assertTrue(pluginContext.findService(Comparable.class).isPresent());
        assertEquals(1, registry.getAll().size());

        manager.stopAll();

        assertTrue(pluginContext.findService(Comparable.class).isEmpty());
        assertSame(applicationService, pluginContext.requireService(Runnable.class));
        assertTrue(registry.getAll().isEmpty());
    }

    @Test
    void stopsPluginAndTransitiveDependentsInReverseLoadOrder() {
        List<String> lifecycleEvents = new ArrayList<>();
        TestPlugin core = new TestPlugin("core", List.of(), lifecycleEvents);
        TestPlugin feature = new TestPlugin("feature", List.of("core"), lifecycleEvents);
        TestPlugin leaf = new TestPlugin("leaf", List.of("feature"), lifecycleEvents);
        TestPlugin independent = new TestPlugin("independent", List.of(), lifecycleEvents);

        PluginLoader pluginLoader = new PluginLoader() {
            @Override
            public List<PluginCandidate> loadPluginCandidates() {
                return candidates(leaf, independent, feature, core);
            }
        };
        DefaultPacketInterceptorRegistry registry = new DefaultPacketInterceptorRegistry();
        PluginManager manager = new PluginManager(
                pluginLoader,
                new PluginDependencyResolver(),
                registry,
                new DefaultPluginContext(registry),
                testContainers()
        );

        manager.loadAndStart();

        assertEquals(List.of("leaf", "feature", "core"), manager.stopPlugin("core"));
        assertEquals(List.of(independent), manager.getLoadedPlugins());
        assertEquals(
                List.of(
                        "load:core",
                        "load:feature",
                        "load:leaf",
                        "load:independent",
                        "start:core",
                        "start:feature",
                        "start:leaf",
                        "start:independent",
                        "stop:leaf",
                        "stop:feature",
                        "stop:core"
                ),
                lifecycleEvents
        );

        manager.stopAll();

        assertEquals("stop:independent", lifecycleEvents.getLast());
        assertEquals(4, lifecycleEvents.stream().filter(event -> event.startsWith("stop:")).count());
    }

    @Test
    void rejectsStoppingUnknownPlugin() {
        DefaultPacketInterceptorRegistry registry = new DefaultPacketInterceptorRegistry();
        PluginManager manager = new PluginManager(
                new PluginLoader(),
                new PluginDependencyResolver(),
                registry,
                new DefaultPluginContext(registry),
                testContainers()
        );

        assertThrows(NoSuchElementException.class, () -> manager.stopPlugin("missing"));
    }

    @Test
    void startsPluginAndMissingDependenciesInLoadOrder() {
        List<String> lifecycleEvents = new ArrayList<>();
        TestPlugin core = new TestPlugin("core", List.of(), lifecycleEvents);
        TestPlugin feature = new TestPlugin("feature", List.of("core"), lifecycleEvents);
        TestPlugin leaf = new TestPlugin("leaf", List.of("feature"), lifecycleEvents);
        TestPlugin independent = new TestPlugin("independent", List.of(), lifecycleEvents);

        PluginLoader pluginLoader = new PluginLoader() {
            @Override
            public List<PluginCandidate> loadPluginCandidates() {
                return candidates(leaf, independent, feature, core);
            }
        };
        DefaultPacketInterceptorRegistry registry = new DefaultPacketInterceptorRegistry();
        PluginManager manager = new PluginManager(
                pluginLoader,
                new PluginDependencyResolver(),
                registry,
                new DefaultPluginContext(registry),
                testContainers()
        );

        manager.loadAndStart();
        manager.stopPlugin("core");
        lifecycleEvents.clear();

        assertEquals(List.of("core", "feature", "leaf"), manager.startPlugin("leaf"));
        assertEquals(List.of(independent, core, feature, leaf), manager.getLoadedPlugins());
        assertEquals(
                List.of(
                        "load:core",
                        "load:feature",
                        "load:leaf",
                        "start:core",
                        "start:feature",
                        "start:leaf"
                ),
                lifecycleEvents
        );
        assertTrue(manager.startPlugin("leaf").isEmpty());

        manager.stopAll();
    }

    @Test
    void rejectsStartingUnavailablePlugin() {
        DefaultPacketInterceptorRegistry registry = new DefaultPacketInterceptorRegistry();
        PluginManager manager = new PluginManager(
                new PluginLoader() {
                    @Override
                    public List<PluginCandidate> loadPluginCandidates() {
                        return List.of();
                    }
                },
                new PluginDependencyResolver(),
                registry,
                new DefaultPluginContext(registry),
                testContainers()
        );

        assertThrows(NoSuchElementException.class, () -> manager.startPlugin("missing"));
    }

    @Test
    void reloadsPluginAndPreviouslyActiveDependents() {
        List<String> lifecycleEvents = new ArrayList<>();
        TestPlugin core = new TestPlugin("core", List.of(), lifecycleEvents);
        TestPlugin feature = new TestPlugin("feature", List.of("core"), lifecycleEvents);
        TestPlugin leaf = new TestPlugin("leaf", List.of("feature"), lifecycleEvents);
        TestPlugin independent = new TestPlugin("independent", List.of(), lifecycleEvents);
        List<String> reloadedPluginIds = new ArrayList<>();

        PluginLoader pluginLoader = new PluginLoader() {
            @Override
            public List<PluginCandidate> loadPluginCandidates() {
                return candidates(leaf, independent, feature, core);
            }

            @Override
            public void reloadPluginCandidates(List<PluginCandidate> plugins) {
                reloadedPluginIds.addAll(plugins.stream().map(plugin -> plugin.descriptor().id()).toList());
            }
        };
        DefaultPacketInterceptorRegistry registry = new DefaultPacketInterceptorRegistry();
        PluginManager manager = new PluginManager(
                pluginLoader,
                new PluginDependencyResolver(),
                registry,
                new DefaultPluginContext(registry),
                testContainers()
        );

        manager.loadAndStart();
        lifecycleEvents.clear();

        assertEquals(List.of("core", "feature", "leaf"), manager.reloadPlugin("core"));
        assertEquals(List.of("leaf", "feature", "core"), reloadedPluginIds);
        assertEquals(List.of(independent, core, feature, leaf), manager.getLoadedPlugins());
        assertEquals(
                List.of(
                        "stop:leaf",
                        "stop:feature",
                        "stop:core",
                        "load:core",
                        "load:feature",
                        "load:leaf",
                        "start:core",
                        "start:feature",
                        "start:leaf"
                ),
                lifecycleEvents
        );

        manager.stopAll();
    }

    @Test
    void validatesReloadBeforeStoppingPlugins() {
        List<String> lifecycleEvents = new ArrayList<>();
        TestPlugin core = new TestPlugin("core", List.of(), lifecycleEvents);
        TestPlugin feature = new TestPlugin("feature", List.of("core"), lifecycleEvents);

        PluginLoader pluginLoader = new PluginLoader() {
            @Override
            public List<PluginCandidate> loadPluginCandidates() {
                return candidates(feature, core);
            }

            @Override
            public void validateReloadPluginCandidates(List<PluginCandidate> plugins) {
                throw new IllegalStateException("reload not supported");
            }
        };
        DefaultPacketInterceptorRegistry registry = new DefaultPacketInterceptorRegistry();
        PluginManager manager = new PluginManager(
                pluginLoader,
                new PluginDependencyResolver(),
                registry,
                new DefaultPluginContext(registry),
                testContainers()
        );

        manager.loadAndStart();
        lifecycleEvents.clear();

        assertThrows(IllegalStateException.class, () -> manager.reloadPlugin("core"));
        assertEquals(List.of(core, feature), manager.getLoadedPlugins());
        assertTrue(lifecycleEvents.isEmpty());

        manager.stopAll();
    }

    @Test
    void exposesLoadedAndStoppedPluginsThroughRuntimeApi() {
        TestPlugin core = new TestPlugin("core", List.of(), new ArrayList<>());
        TestPlugin feature = new TestPlugin("feature", List.of("core"), new ArrayList<>());
        PluginLoader pluginLoader = new PluginLoader() {
            @Override
            public List<PluginCandidate> loadPluginCandidates() {
                return candidates(feature, core);
            }
        };
        DefaultPacketInterceptorRegistry registry = new DefaultPacketInterceptorRegistry();
        PluginManager manager = new PluginManager(
                pluginLoader,
                new PluginDependencyResolver(),
                registry,
                new DefaultPluginContext(registry),
                testContainers()
        );

        manager.loadAndStart();
        manager.stopPlugin("feature");

        assertEquals(
                List.of(
                        new PluginRuntimeView(feature.descriptor(), PluginRuntimeState.STOPPED),
                        new PluginRuntimeView(core.descriptor(), PluginRuntimeState.LOADED)
                ),
                manager.plugins()
        );

        manager.stopAll();
    }

    private static final Map<PluginCandidate, ProxyPlugin> TEST_PLUGINS =
            Collections.synchronizedMap(new IdentityHashMap<>());

    private static List<PluginCandidate> candidates(ProxyPlugin... plugins) {
        return java.util.Arrays.stream(plugins)
                .map(plugin -> {
                    PluginCandidate candidate = new PluginCandidate(
                            plugin.getClass().asSubclass(ProxyPlugin.class),
                            plugin.descriptor()
                    );
                    TEST_PLUGINS.put(candidate, plugin);
                    return candidate;
                })
                .toList();
    }

    private static ProxyPlugin pluginFor(PluginCandidate candidate) {
        ProxyPlugin plugin = TEST_PLUGINS.get(candidate);
        if (plugin == null) {
            throw new IllegalStateException("No test plugin instance for " + candidate.descriptor().id());
        }
        return plugin;
    }

    private static PluginContainerFactory testContainers() {
        return (candidate, context) -> new PluginContainer() {
            @Override
            public ProxyPlugin plugin() {
                return pluginFor(candidate);
            }

            @Override
            public void close() {
            }
        };
    }

    private static class TestPlugin implements ProxyPlugin {

        private final PluginDescriptor descriptor;
        private final List<String> lifecycleEvents;

        private TestPlugin(String id, List<String> dependsOn, List<String> lifecycleEvents) {
            this.descriptor = new PluginDescriptor(id, id, "1.0.0", dependsOn);
            this.lifecycleEvents = lifecycleEvents;
        }

        @Override
        public PluginDescriptor descriptor() {
            return descriptor;
        }

        @Override
        public void onLoad(PluginContext context) {
            lifecycleEvents.add("load:" + descriptor.id());
        }

        @Override
        public void onStart() {
            lifecycleEvents.add("start:" + descriptor.id());
        }

        @Override
        public void onStop() {
            lifecycleEvents.add("stop:" + descriptor.id());
        }
    }

    private static class SessionLifecyclePlugin implements ProxyPlugin {

        private final PluginDescriptor descriptor;
        private final List<String> lifecycleEvents;

        private SessionLifecyclePlugin(String id, List<String> dependsOn, List<String> lifecycleEvents) {
            this.descriptor = new PluginDescriptor(id, id, "1.0.0", dependsOn);
            this.lifecycleEvents = lifecycleEvents;
        }

        @Override
        public PluginDescriptor descriptor() {
            return descriptor;
        }

        @Override
        public void onLoad(PluginContext context) {
            lifecycleEvents.add("load:" + descriptor.id());
        }

        @Override
        public void onStart() {
            lifecycleEvents.add("start:" + descriptor.id());
        }

        @Override
        public void onConnectionSuccess(PluginSessionContext context) {
            lifecycleEvents.add("connected:" + descriptor.id() + ":" + context.sessionId());
        }

        @Override
        public void onDisconnecting(PluginSessionContext context) {
            lifecycleEvents.add("disconnecting:" + descriptor.id() + ":" + context.sessionId());
        }

        @Override
        public void onDisconnected(PluginSessionContext context) {
            lifecycleEvents.add("disconnected:" + descriptor.id() + ":" + context.sessionId());
        }
    }

    private static final class RecordingSessionPermissionService implements SessionPermissionService {
        private PermissionView permissions = PermissionView.EMPTY;

        @Override
        public PermissionView permissions(String sessionId) {
            return permissions;
        }

        @Override
        public void updatePermissions(String sessionId, PermissionView permissions) {
            this.permissions = permissions;
        }

        @Override
        public void clearPermissions(String sessionId) {
            permissions = PermissionView.EMPTY;
        }
    }

    private static final class ComparableService implements Comparable<ComparableService> {
        @Override
        public int compareTo(ComparableService ignored) {
            return 0;
        }
    }
}

