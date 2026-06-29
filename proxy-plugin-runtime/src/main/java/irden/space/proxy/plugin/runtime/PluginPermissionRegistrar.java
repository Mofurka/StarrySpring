package irden.space.proxy.plugin.runtime;

import irden.space.proxy.plugin.api.PermissionEnum;
import irden.space.proxy.plugin.api.PluginContext;
import irden.space.proxy.plugin.api.annotations.RegisterPluginPermissions;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public final class PluginPermissionRegistrar {

    private PluginPermissionRegistrar() {
    }

    public static void register(Object bean, PluginContext context) {
        Objects.requireNonNull(bean, "bean");
        Objects.requireNonNull(context, "context");

        Method method = PluginAnnotatedMethods.find(bean.getClass(), RegisterPluginPermissions.class);
        if (method == null) {
            return;
        }

        Object registrationResult = PluginAnnotatedMethods.invoke(bean, method, context);
        PermissionEnum.registerDefaults(resolveRegisteredPermissionTypes(method, registrationResult));
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
            List<Class<? extends PermissionEnum>> resolvedTypes = new ArrayList<>(permissionTypes.length);
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
        List<Class<? extends PermissionEnum>> resolvedTypes = new ArrayList<>();
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
}
