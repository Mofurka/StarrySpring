package irden.space.proxy.plugin.api;

import irden.space.proxy.plugin.api.annotations.*;

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

    public static void registerPluginPermissions(Object bean, PluginContext context) {
        Objects.requireNonNull(bean, "bean");
        Objects.requireNonNull(context, "context");

        Method lifecycleMethod = findLifecycleMethod(bean.getClass(), RegisterPluginPermissions.class);
        if (lifecycleMethod == null) {
            return;
        }

        if (!lifecycleMethod.trySetAccessible()) {
            throw new IllegalStateException("Cannot access lifecycle method " + lifecycleMethod);
        }

        Object registrationResult = invokeMethod(bean, lifecycleMethod, context);
        PermissionEnum.registerDefaults(resolveRegisteredPermissionTypes(lifecycleMethod, registrationResult));
    }

    public static void onLoad(Object bean, PluginContext context) {
        Objects.requireNonNull(bean, "bean");
        Objects.requireNonNull(context, "context");

        invokeLifecycle(bean, OnLoad.class, context);
    }

    public static void onStart(Object bean) {
        Objects.requireNonNull(bean, "bean");
        invokeLifecycle(bean, OnStart.class);
    }

    public static void onConnectionSuccess(Object bean, PluginSessionContext context) {
        Objects.requireNonNull(bean, "bean");
        Objects.requireNonNull(context, "context");
        invokeLifecycle(bean, OnConnectionSuccess.class, context);
    }

    public static void onDisconnecting(Object bean, PluginSessionContext context) {
        Objects.requireNonNull(bean, "bean");
        Objects.requireNonNull(context, "context");
        invokeLifecycle(bean, OnDisconnecting.class, context);
    }

    public static void onDisconnected(Object bean, PluginSessionContext context) {
        Objects.requireNonNull(bean, "bean");
        Objects.requireNonNull(context, "context");
        invokeLifecycle(bean, OnDisconnected.class, context);
    }

    public static void onStop(Object bean) {
        Objects.requireNonNull(bean, "bean");
        invokeLifecycle(bean, OnStop.class);
    }

    private static void invokeLifecycle(Object bean, Class<? extends Annotation> annotationType, Object... arguments) {
        Method lifecycleMethod = findLifecycleMethod(bean.getClass(), annotationType);
        if (lifecycleMethod == null) {
            return;
        }

        if (!lifecycleMethod.trySetAccessible()) {
            throw new IllegalStateException("Cannot access lifecycle method " + lifecycleMethod);
        }

        invokeMethod(bean, lifecycleMethod, arguments);
    }

    private static Object invokeMethod(Object bean, Method lifecycleMethod, Object... arguments) {
        try {
            if (lifecycleMethod.getParameterCount() == 0) {
                return lifecycleMethod.invoke(bean);
            }

            return lifecycleMethod.invoke(bean, arguments);
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

    private static List<Class<? extends PermissionEnum>> resolveRegisteredPermissionTypes(Method method, Object registrationResult) {
        if (void.class.equals(method.getReturnType())) {
            return List.of();
        }

        if (registrationResult == null) {
            throw new IllegalStateException("@RegisterPluginPermissions must not return null: " + method);
        }

        if (registrationResult instanceof Class<?> permissionType) {
            return List.of(asPermissionType(method, permissionType));
        }

        if (registrationResult instanceof Class<?>[] permissionTypes) {
            List<Class<? extends PermissionEnum>> resolvedTypes = new java.util.ArrayList<>(permissionTypes.length);
            for (Class<?> permissionType : permissionTypes) {
                resolvedTypes.add(asPermissionType(method, permissionType));
            }
            return List.copyOf(resolvedTypes);
        }

        if (registrationResult instanceof Iterable<?> permissionTypes) {
            return toPermissionTypes(method, permissionTypes);
        }

        throw new IllegalArgumentException(
                "@RegisterPluginPermissions must return void, Class<? extends PermissionEnum>, Class<? extends PermissionEnum>[] or Iterable<Class<? extends PermissionEnum>>: "
                        + method
        );
    }

    private static List<Class<? extends PermissionEnum>> toPermissionTypes(Method method, Iterable<?> permissionTypes) {
        List<Class<? extends PermissionEnum>> resolvedTypes = new java.util.ArrayList<>();
        for (Object permissionType : permissionTypes) {
            resolvedTypes.add(asPermissionType(method, permissionType));
        }
        return List.copyOf(resolvedTypes);
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends PermissionEnum> asPermissionType(Method method, Object candidate) {
        if (!(candidate instanceof Class<?> permissionType) || !PermissionEnum.class.isAssignableFrom(permissionType)) {
            throw new IllegalArgumentException(
                    "@RegisterPluginPermissions entries must be PermissionEnum classes: " + method
            );
        }

        return (Class<? extends PermissionEnum>) permissionType;
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

        if (annotationType.equals(RegisterPluginPermissions.class)) {
            if (method.getParameterCount() != 0
                    && !(method.getParameterCount() == 1 && PluginContext.class.equals(method.getParameterTypes()[0]))) {
                throw new IllegalArgumentException(
                        "@RegisterPluginPermissions method must declare no arguments or a single PluginContext parameter: " + method
                );
            }

            if (void.class.equals(method.getReturnType())
                    || Class.class.equals(method.getReturnType())
                    || method.getReturnType().isArray() && Class.class.equals(method.getReturnType().getComponentType())
                    || Iterable.class.isAssignableFrom(method.getReturnType())) {
                return;
            }

            throw new IllegalArgumentException(
                    "@RegisterPluginPermissions method must return void, Class<? extends PermissionEnum>, Class<? extends PermissionEnum>[] or Iterable<Class<? extends PermissionEnum>>: " + method
            );
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

        if (annotationType.equals(OnConnectionSuccess.class)
                || annotationType.equals(OnDisconnecting.class)
                || annotationType.equals(OnDisconnected.class)) {
            if (method.getParameterCount() == 0) {
                return;
            }

            if (method.getParameterCount() == 1 && PluginSessionContext.class.equals(method.getParameterTypes()[0])) {
                return;
            }

            throw new IllegalArgumentException(
                    "@" + annotationType.getSimpleName()
                            + " method must declare no arguments or a single PluginSessionContext parameter: " + method
            );
        }

        if (method.getParameterCount() != 0) {
            throw new IllegalArgumentException(annotationType.getSimpleName() + " method must declare no parameters: " + method);
        }
    }
}
