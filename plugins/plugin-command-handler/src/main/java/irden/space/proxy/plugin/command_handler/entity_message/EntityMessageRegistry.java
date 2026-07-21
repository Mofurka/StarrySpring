package irden.space.proxy.plugin.command_handler.entity_message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


public class EntityMessageRegistry {

    private static final EntityMessageRegistry GLOBAL = new EntityMessageRegistry();

    private static final Logger log = LoggerFactory.getLogger(EntityMessageRegistry.class);

    private final Map<String, RegisteredEntityMessageHandler> handlersByMessage = new LinkedHashMap<>();

    public static EntityMessageRegistry global() {
        return GLOBAL;
    }

    public synchronized RegisteredEntityMessageHandler register(
            String ownerPluginId,
            Object bean,
            Method method,
            EntityMessageHandler annotation
    ) {
        Objects.requireNonNull(ownerPluginId, "ownerPluginId");
        Objects.requireNonNull(bean, "bean");
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(annotation, "annotation");

        String message = annotation.value() == null ? "" : annotation.value().trim();
        if (message.isEmpty()) {
            throw new IllegalArgumentException("@EntityMessageHandler name must not be blank on method " + method);
        }

        RegisteredEntityMessageHandler existing = handlersByMessage.get(message);
        if (existing != null) {
            throw new IllegalStateException(
                    "EntityMessage '" + message + "' is already handled by plugin '" + existing.ownerPluginId() + "'");
        }

        RegisteredEntityMessageHandler handler = new RegisteredEntityMessageHandler(
                ownerPluginId,
                message,
                annotation.description().trim(),
                bean,
                method
        );

        handlersByMessage.put(message, handler);
        log.info("Registered EntityMessage handler '{}' from plugin '{}'", message, ownerPluginId);
        return handler;
    }

    public synchronized RegisteredEntityMessageHandler find(String message) {
        if (message == null) {
            return null;
        }
        return handlersByMessage.get(message);
    }

    public synchronized void unregisterByPluginId(String pluginId) {
        if (pluginId == null) {
            return;
        }
        handlersByMessage.entrySet().removeIf(entry -> {
            boolean owned = pluginId.equals(entry.getValue().ownerPluginId());
            if (owned) {
                log.info("Unregistered EntityMessage handler '{}' from plugin '{}'", entry.getKey(), pluginId);
            }
            return owned;
        });
    }

    public synchronized Collection<RegisteredEntityMessageHandler> all() {
        return List.copyOf(handlersByMessage.values());
    }
}
