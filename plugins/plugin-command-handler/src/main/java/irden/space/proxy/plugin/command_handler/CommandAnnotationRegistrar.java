package irden.space.proxy.plugin.command_handler;

import irden.space.proxy.plugin.api.PluginAnnotationRegistrar;
import irden.space.proxy.plugin.api.PluginContext;
import irden.space.proxy.plugin.api.ProxyPlugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

public class CommandAnnotationRegistrar implements PluginAnnotationRegistrar {

    @Override
    public boolean supports(Class<?> pluginType) {
        return Arrays.stream(pluginType.getDeclaredMethods())
                .anyMatch(method -> method.isAnnotationPresent(ChatCommand.class));
    }

    @Override
    public void register(ProxyPlugin plugin, PluginContext context) {
        Arrays.stream(plugin.getClass().getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(ChatCommand.class))
                .forEach(method -> register(plugin, method));
    }

    private void register(ProxyPlugin plugin, Method method) {
        validateMethod(method);

        if (!method.trySetAccessible()) {
            throw new IllegalStateException("Cannot access @ChatCommand method " + method);
        }

        ChatCommand annotation = method.getAnnotation(ChatCommand.class);
        CommandSpec spec = invokeSpecFactory(plugin, method);

        CommandRegistry.global().register(plugin, method, annotation, spec);
    }

    private CommandSpec invokeSpecFactory(ProxyPlugin plugin, Method method) {
        try {
            Object result = method.invoke(plugin);

            if (!(result instanceof CommandSpec spec)) {
                throw new IllegalStateException("@ChatCommand method must return CommandSpec: " + method);
            }

            return spec;
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();

            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }

            if (cause instanceof Error error) {
                throw error;
            }

            throw new IllegalStateException("Failed to create CommandSpec from @ChatCommand method " + method, cause);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to create CommandSpec from @ChatCommand method " + method, e);
        }
    }

    private void validateMethod(Method method) {
        if (Modifier.isStatic(method.getModifiers())) {
            throw new IllegalArgumentException("@ChatCommand method must not be static: " + method);
        }

        if (!CommandSpec.class.equals(method.getReturnType())) {
            throw new IllegalArgumentException("@ChatCommand method must return CommandSpec: " + method);
        }

        if (method.getParameterCount() != 0) {
            throw new IllegalArgumentException("@ChatCommand method must declare no parameters: " + method);
        }
    }
}