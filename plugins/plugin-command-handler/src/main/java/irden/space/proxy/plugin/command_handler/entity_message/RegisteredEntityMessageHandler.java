package irden.space.proxy.plugin.command_handler.entity_message;

import irden.space.proxy.protocol.codec.variant.NullVariantValue;
import irden.space.proxy.protocol.codec.variant.VariantValue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public record RegisteredEntityMessageHandler(
        String ownerPluginId,
        String message,
        String description,
        Object bean,
        Method method
) {


    public VariantValue invoke(EntityMessageContext context) {
        Object result;
        try {
            result = method.invoke(bean, context);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("EntityMessage handler '" + message + "' failed", cause);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to invoke EntityMessage handler '" + message + "'", e);
        }

        if (result == null) {
            return NullVariantValue.INSTANCE;
        }
        if (result instanceof VariantValue variantValue) {
            return variantValue;
        }
        throw new IllegalStateException(
                "@EntityMessageHandler method must return VariantValue or void: " + method);
    }
}
