package irden.space.proxy.plugin.runtime;


import irden.space.proxy.plugin.api.PacketInterceptorRegistry;
import irden.space.proxy.plugin.api.PluginContext;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultPluginContext implements PluginContext {

    private final PacketInterceptorRegistry packetInterceptorRegistry;
    private final Map<Class<?>, Object> services = new ConcurrentHashMap<>();

    public DefaultPluginContext(PacketInterceptorRegistry packetInterceptorRegistry) {
        this.packetInterceptorRegistry = packetInterceptorRegistry;
    }

    @Override
    public PacketInterceptorRegistry packetInterceptorRegistry() {
        return packetInterceptorRegistry;
    }

    @Override
    public <T> void publishService(Class<T> serviceType, T service) {
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
        services.put(serviceType, service);
    }

    @Override
    public <T> Optional<T> findService(Class<T> serviceType) {
        if (serviceType == null) {
            return Optional.empty();
        }
        Object service = services.get(serviceType);
        if (service == null) {
            return Optional.empty();
        }
        return Optional.of(serviceType.cast(service));
    }

    @Override
    public void removeService(Class<?> serviceType) {
        if (serviceType != null) {
            services.remove(serviceType);
        }
    }


}
