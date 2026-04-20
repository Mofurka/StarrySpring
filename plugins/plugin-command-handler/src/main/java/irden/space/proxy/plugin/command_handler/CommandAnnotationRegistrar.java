package irden.space.proxy.plugin.command_handler;

import irden.space.proxy.plugin.api.PluginAnnotationRegistrar;
import irden.space.proxy.plugin.api.PluginContext;
import irden.space.proxy.plugin.api.ProxyPlugin;

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
        CommandRegistry.global().register(plugin, method, annotation);
    }

    private void validateMethod(Method method) {
        if (Modifier.isStatic(method.getModifiers())) {
            throw new IllegalArgumentException("@ChatCommand method must not be static: " + method);
        }

        if (!void.class.equals(method.getReturnType())) {
            throw new IllegalArgumentException("@ChatCommand method must return void: " + method);
        }

        if (method.getParameterCount() == 0) {
            return;
        }

        if (method.getParameterCount() == 1 && CommandContext.class.equals(method.getParameterTypes()[0])) {
            return;
        }

        throw new IllegalArgumentException(
                "@ChatCommand method must declare no parameters or a single CommandContext parameter: " + method
        );
    }
}
