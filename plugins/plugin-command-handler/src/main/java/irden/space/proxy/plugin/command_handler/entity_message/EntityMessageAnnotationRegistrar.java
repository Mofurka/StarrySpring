package irden.space.proxy.plugin.command_handler.entity_message;

import irden.space.proxy.plugin.api.PluginAnnotationRegistrar;
import irden.space.proxy.plugin.api.PluginContext;
import irden.space.proxy.plugin.api.PluginDescriptor;
import irden.space.proxy.protocol.codec.variant.VariantValue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

public class EntityMessageAnnotationRegistrar implements PluginAnnotationRegistrar {

    @Override
    public boolean supports(Class<?> pluginType) {
        return Arrays.stream(pluginType.getDeclaredMethods())
                .anyMatch(method -> method.isAnnotationPresent(EntityMessageHandler.class));
    }

    @Override
    public void register(Object bean, PluginDescriptor owner, PluginContext context) {
        context.onRemove(() -> EntityMessageRegistry.global().unregisterByPluginId(owner.id()));

        Arrays.stream(bean.getClass().getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(EntityMessageHandler.class))
                .forEach(method -> register(bean, owner, method));
    }

    private void register(Object bean, PluginDescriptor owner, Method method) {
        validateMethod(method);

        if (!method.trySetAccessible()) {
            throw new IllegalStateException("Cannot access @EntityMessageHandler method " + method);
        }

        EntityMessageRegistry.global().register(
                owner.id(),
                bean,
                method,
                method.getAnnotation(EntityMessageHandler.class)
        );
    }

    private void validateMethod(Method method) {
        if (Modifier.isStatic(method.getModifiers())) {
            throw new IllegalArgumentException("@EntityMessageHandler method must not be static: " + method);
        }

        if (method.getParameterCount() != 1
                || !EntityMessageContext.class.equals(method.getParameterTypes()[0])) {
            throw new IllegalArgumentException(
                    "@EntityMessageHandler method must declare exactly one EntityMessageContext parameter: " + method);
        }

        Class<?> returnType = method.getReturnType();
        if (!void.class.equals(returnType) && !VariantValue.class.isAssignableFrom(returnType)) {
            throw new IllegalArgumentException(
                    "@EntityMessageHandler method must return VariantValue or void: " + method);
        }
    }
}
