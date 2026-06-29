package irden.space.proxy.plugin.runtime;


import irden.space.proxy.plugin.api.PacketInterceptor;
import irden.space.proxy.plugin.api.PacketInterceptorRegistry;
import irden.space.proxy.plugin.api.PluginContext;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class DefaultPluginContext implements PluginContextManager {

    private final PacketInterceptorRegistry packetInterceptorRegistry;
    private final Map<String, Set<PacketInterceptor>> interceptorsByPlugin = new ConcurrentHashMap<>();
    private final Map<String, ConcurrentLinkedDeque<Runnable>> cleanupByPlugin = new ConcurrentHashMap<>();

    public DefaultPluginContext(PacketInterceptorRegistry packetInterceptorRegistry) {
        this.packetInterceptorRegistry = packetInterceptorRegistry;
    }

    @Override
    public PacketInterceptorRegistry packetInterceptorRegistry() {
        return packetInterceptorRegistry;
    }

    @Override
    public PluginContext forPlugin(String pluginId) {
        if (pluginId == null || pluginId.isBlank()) {
            throw new IllegalArgumentException("Plugin id must not be blank");
        }

        return new PluginContext() {
            private final PacketInterceptorRegistry ownedInterceptorRegistry = new PacketInterceptorRegistry() {
                @Override
                public void register(PacketInterceptor interceptor) {
                    Objects.requireNonNull(interceptor, "interceptor");
                    packetInterceptorRegistry.register(interceptor);
                    interceptorsByPlugin.computeIfAbsent(pluginId, ignored -> ConcurrentHashMap.newKeySet())
                            .add(interceptor);
                }

                @Override
                public void unregister(PacketInterceptor interceptor) {
                    packetInterceptorRegistry.unregister(interceptor);
                    Set<PacketInterceptor> ownedInterceptors = interceptorsByPlugin.get(pluginId);
                    if (ownedInterceptors != null) {
                        ownedInterceptors.remove(interceptor);
                    }
                }

                @Override
                public java.util.List<PacketInterceptor> getAll() {
                    return packetInterceptorRegistry.getAll();
                }
            };

            @Override
            public PacketInterceptorRegistry packetInterceptorRegistry() {
                return ownedInterceptorRegistry;
            }

            @Override
            public void onRemove(Runnable cleanup) {
                Objects.requireNonNull(cleanup, "cleanup");
                cleanupByPlugin.computeIfAbsent(pluginId, ignored -> new ConcurrentLinkedDeque<>())
                        .addFirst(cleanup);
            }
        };
    }

    @Override
    public void removePlugin(String pluginId) {
        if (pluginId == null) {
            return;
        }

        RuntimeException cleanupFailure = runCleanupCallbacks(pluginId);
        Set<PacketInterceptor> interceptors = interceptorsByPlugin.remove(pluginId);
        if (interceptors != null) {
            interceptors.forEach(packetInterceptorRegistry::unregister);
        }

        if (cleanupFailure != null) {
            throw cleanupFailure;
        }
    }

    private RuntimeException runCleanupCallbacks(String pluginId) {
        ConcurrentLinkedDeque<Runnable> cleanupTasks = cleanupByPlugin.remove(pluginId);
        if (cleanupTasks == null) {
            return null;
        }

        RuntimeException firstFailure = null;
        for (Runnable cleanupTask : cleanupTasks) {
            try {
                cleanupTask.run();
            } catch (RuntimeException e) {
                if (firstFailure == null) {
                    firstFailure = e;
                } else {
                    firstFailure.addSuppressed(e);
                }
            }
        }
        return firstFailure;
    }
}
