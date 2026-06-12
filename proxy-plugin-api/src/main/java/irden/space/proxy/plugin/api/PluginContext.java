package irden.space.proxy.plugin.api;

import java.util.NoSuchElementException;
import java.util.Optional;

public interface PluginContext {

    PacketInterceptorRegistry packetInterceptorRegistry();

    default <T> void publishService(Class<T> serviceType, T service) {
        throw new UnsupportedOperationException("Service registry is not supported by this PluginContext implementation");
    }

    default <T> Optional<T> findService(Class<T> serviceType) {
        return Optional.empty();
    }

    default <T> T requireService(Class<T> serviceType) {
        return findService(serviceType)
                .orElseThrow(() -> new NoSuchElementException(
                        "Service '%s' is not registered".formatted(serviceType.getName())
                ));
    }

    default void onRemove(Runnable cleanup) {
        // Optional lifecycle hook for managed plugin contexts.
    }

    void removeService(Class<?> serviceType);
}
