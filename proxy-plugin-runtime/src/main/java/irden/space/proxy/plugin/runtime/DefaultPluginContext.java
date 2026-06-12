package irden.space.proxy.plugin.runtime;


import irden.space.proxy.plugin.api.PacketInterceptor;
import irden.space.proxy.plugin.api.PacketInterceptorRegistry;
import irden.space.proxy.plugin.api.PluginContext;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class DefaultPluginContext implements PluginContextManager, PluginServiceProvider {

    private final PacketInterceptorRegistry packetInterceptorRegistry;
    private final Map<Class<?>, ServiceRegistration> services = new ConcurrentHashMap<>();
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
    public <T> void publishService(Class<T> serviceType, T service) {
        publishService(null, serviceType, service);
    }

    private <T> void publishService(String pluginId, Class<T> serviceType, T service) {
        if (serviceType == null) {
            throw new IllegalArgumentException("Service type must not be null");
        }
        if (service == null) {
            throw new IllegalArgumentException("Service instance must not be null");
        }
        if (!serviceType.isInstance(service)) {
            throw new IllegalArgumentException(
                    "Service %s is not an instance of %s".formatted(service.getClass().getName(), serviceType.getName())
            );
        }
        services.compute(serviceType, (ignored, existing) -> {
            if (existing != null && !Objects.equals(existing.pluginId(), pluginId)) {
                throw new IllegalStateException(
                        "Service '%s' is already published by %s".formatted(
                                serviceType.getName(),
                                existing.pluginId() == null ? "the application" : "plugin '" + existing.pluginId() + "'"
                        )
                );
            }
            return new ServiceRegistration(pluginId, service);
        });
    }

    @Override
    public <T> Optional<T> findService(Class<T> serviceType) {
        if (serviceType == null) {
            return Optional.empty();
        }
        ServiceRegistration registration = services.get(serviceType);
        if (registration == null) {
            return Optional.empty();
        }
        return Optional.of(serviceType.cast(registration.service()));
    }

    @Override
    public void removeService(Class<?> serviceType) {
        if (serviceType != null) {
            services.remove(serviceType);
        }
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
            public <T> void publishService(Class<T> serviceType, T service) {
                DefaultPluginContext.this.publishService(pluginId, serviceType, service);
            }

            @Override
            public <T> Optional<T> findService(Class<T> serviceType) {
                return DefaultPluginContext.this.findService(serviceType);
            }

            @Override
            public void onRemove(Runnable cleanup) {
                Objects.requireNonNull(cleanup, "cleanup");
                cleanupByPlugin.computeIfAbsent(pluginId, ignored -> new ConcurrentLinkedDeque<>())
                        .addFirst(cleanup);
            }

            @Override
            public void removeService(Class<?> serviceType) {
                if (serviceType != null) {
                    services.computeIfPresent(
                            serviceType,
                            (ignored, registration) -> pluginId.equals(registration.pluginId()) ? null : registration
                    );
                }
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
        services.entrySet().removeIf(entry -> pluginId.equals(entry.getValue().pluginId()));

        if (cleanupFailure != null) {
            throw cleanupFailure;
        }
    }

    @Override
    public Map<Class<?>, Object> servicesPublishedBy(java.util.Collection<String> pluginIds) {
        if (pluginIds == null || pluginIds.isEmpty()) {
            return Map.of();
        }

        Set<String> requestedPluginIds = Set.copyOf(pluginIds);
        Map<Class<?>, Object> result = new java.util.LinkedHashMap<>();
        services.forEach((serviceType, registration) -> {
            if (registration.pluginId() != null && requestedPluginIds.contains(registration.pluginId())) {
                result.put(serviceType, registration.service());
            }
        });
        return Map.copyOf(result);
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

    private record ServiceRegistration(String pluginId, Object service) {
    }
}
