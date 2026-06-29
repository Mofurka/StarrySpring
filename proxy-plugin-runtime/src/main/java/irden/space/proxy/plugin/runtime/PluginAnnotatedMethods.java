package irden.space.proxy.plugin.runtime;

import irden.space.proxy.plugin.api.PluginContext;
import irden.space.proxy.plugin.api.PluginSessionContext;
import irden.space.proxy.plugin.api.annotations.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;


final class PluginAnnotatedMethods {

    private PluginAnnotatedMethods() {
    }


    static Method find(Class<?> beanType, Class<? extends Annotation> annotationType) {
        List<Method> annotatedMethods = Arrays.stream(beanType.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(annotationType))
                .toList();

        if (annotatedMethods.isEmpty()) {
            return null;
        }

        if (annotatedMethods.size() > 1) {
            throw new IllegalArgumentException(
                    "Plugin class " + beanType.getName() + " declares multiple " + annotationType.getSimpleName() + " methods"
            );
        }

        Method annotatedMethod = annotatedMethods.getFirst();
        validate(annotationType, annotatedMethod);
        return annotatedMethod;
    }

    static Object invoke(Object bean, Method method, Object... arguments) {
        if (!method.trySetAccessible()) {
            throw new IllegalStateException("Cannot access annotated method " + method);
        }

        try {
            if (method.getParameterCount() == 0) {
                return method.invoke(bean);
            }

            return method.invoke(bean, arguments);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("Failed to invoke annotated method " + method, cause);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to invoke annotated method " + method, e);
        }
    }

    private static void validate(Class<? extends Annotation> annotationType, Method method) {
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
