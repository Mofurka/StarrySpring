package irden.space.proxy.plugin.api;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class ProxyPluginSupport {

    private ProxyPluginSupport() {
    }

    public static PluginDescriptor descriptor(ProxyPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");

        PluginDefinition definition = plugin.getClass().getAnnotation(PluginDefinition.class);
        if (definition == null) {
            throw new IllegalStateException(
                    "Plugin class " + plugin.getClass().getName()
                            + " must either override descriptor() or declare @PluginDefinition"
            );
        }

        return new PluginDescriptor(
                definition.id(),
                definition.name(),
                definition.version(),
                List.of(definition.dependsOn())
        );
    }

    public static void onLoad(ProxyPlugin plugin, PluginContext context) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(context, "context");

        if (hasPacketHandlers(plugin.getClass())) {
            context.packetInterceptorRegistry().registerAnnotated(plugin);
        }

        invokeLifecycle(plugin, OnLoad.class, context);
    }

    public static void onStart(ProxyPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        invokeLifecycle(plugin, OnStart.class);
    }

    public static void onStop(ProxyPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        invokeLifecycle(plugin, OnStop.class);
    }

    private static boolean hasPacketHandlers(Class<?> pluginType) {
        return Arrays.stream(pluginType.getDeclaredMethods())
                .anyMatch(method -> method.isAnnotationPresent(PacketHandler.class));
    }

    private static void invokeLifecycle(ProxyPlugin plugin, Class<? extends Annotation> annotationType, Object... arguments) {
        Method lifecycleMethod = findLifecycleMethod(plugin.getClass(), annotationType);
        if (lifecycleMethod == null) {
            return;
        }

        if (!lifecycleMethod.trySetAccessible()) {
            throw new IllegalStateException("Cannot access lifecycle method " + lifecycleMethod);
        }

        try {
            lifecycleMethod.invoke(plugin, arguments);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("Failed to invoke lifecycle method " + lifecycleMethod, cause);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to invoke lifecycle method " + lifecycleMethod, e);
        }
    }

    private static Method findLifecycleMethod(Class<?> pluginType, Class<? extends Annotation> annotationType) {
        List<Method> lifecycleMethods = Arrays.stream(pluginType.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(annotationType))
                .toList();

        if (lifecycleMethods.isEmpty()) {
            return null;
        }

        if (lifecycleMethods.size() > 1) {
            throw new IllegalArgumentException(
                    "Plugin class " + pluginType.getName() + " declares multiple " + annotationType.getSimpleName() + " methods"
            );
        }

        Method lifecycleMethod = lifecycleMethods.getFirst();
        validateLifecycleMethod(annotationType, lifecycleMethod);
        return lifecycleMethod;
    }

    private static void validateLifecycleMethod(Class<? extends Annotation> annotationType, Method method) {
        if (Modifier.isStatic(method.getModifiers())) {
            throw new IllegalArgumentException(annotationType.getSimpleName() + " method must not be static: " + method);
        }

        if (!void.class.equals(method.getReturnType())) {
            throw new IllegalArgumentException(annotationType.getSimpleName() + " method must return void: " + method);
        }

        if (annotationType.equals(OnLoad.class)) {
            if (method.getParameterCount() == 0) {
                return;
            }

            if (method.getParameterCount() == 1 && PluginContext.class.equals(method.getParameterTypes()[0])) {
                return;
            }

            throw new IllegalArgumentException(
                    "@OnLoad method must declare no arguments or a single PluginContext parameter: " + method
            );
        }

        if (method.getParameterCount() != 0) {
            throw new IllegalArgumentException(annotationType.getSimpleName() + " method must declare no parameters: " + method);
        }
    }
}
