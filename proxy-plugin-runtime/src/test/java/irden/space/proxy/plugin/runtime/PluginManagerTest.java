package irden.space.proxy.plugin.runtime;

import irden.space.proxy.plugin.api.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginManagerTest {

    @Test
    void loadAndStartLogsAgainstResolvedDependencyOrder() {
        List<String> lifecycleEvents = new ArrayList<>();

        TestPlugin corePlugin = new TestPlugin("core", List.of(), lifecycleEvents);
        TestPlugin featurePlugin = new TestPlugin("feature", List.of("core"), lifecycleEvents);

        PluginLoader pluginLoader = new PluginLoader() {
            @Override
            public List<ProxyPlugin> loadPlugins() {
                return List.of(featurePlugin, corePlugin);
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
                pluginContext
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
            public List<ProxyPlugin> loadPlugins() {
                return List.of(featurePlugin, corePlugin);
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
                pluginContext
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
            public List<ProxyPlugin> loadPlugins() {
                return List.of(plugin);
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
                pluginContext
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

    private static final class TestPlugin implements ProxyPlugin {

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
}

