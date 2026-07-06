package irden.space.boot;

import irden.space.proxy.plugin.api.PluginContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.handler.AbstractHandlerMethodMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

final class PluginWebEndpointRegistrar {

    private static final Logger log = LoggerFactory.getLogger(PluginWebEndpointRegistrar.class);
    private static final String HANDLER_MAPPING_BEAN_NAME = "requestMappingHandlerMapping";
    private static final Method DETECT_HANDLER_METHODS = resolveDetectHandlerMethods();

    private final ApplicationContext rootContext;

    PluginWebEndpointRegistrar(ApplicationContext rootContext) {
        this.rootContext = Objects.requireNonNull(rootContext, "rootContext");
    }

    void registerControllers(String pluginId, Collection<Object> beans, PluginContext scopedContext) {
        RequestMappingHandlerMapping mapping = resolveHandlerMapping();
        if (mapping == null) {
            return;
        }

        for (Object bean : beans) {
            if (isController(bean.getClass())) {
                registerController(pluginId, mapping, bean, scopedContext);
            }
        }
    }

    private void registerController(
            String pluginId,
            RequestMappingHandlerMapping mapping,
            Object bean,
            PluginContext scopedContext
    ) {
        Set<RequestMappingInfo> before = new HashSet<>(mapping.getHandlerMethods().keySet());

        try {
            DETECT_HANDLER_METHODS.invoke(mapping, bean);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException(
                    "Failed to register web endpoints for controller " + bean.getClass().getName(), cause);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Failed to invoke detectHandlerMethods for controller " + bean.getClass().getName(), e);
        }

        List<RequestMappingInfo> registered = mapping.getHandlerMethods().keySet().stream()
                .filter(info -> !before.contains(info))
                .toList();

        if (registered.isEmpty()) {
            return;
        }

        scopedContext.onRemove(() -> unregister(pluginId, mapping, registered));
        log.info(
                "Registered {} web endpoint(s) from plugin '{}' controller {}",
                registered.size(),
                pluginId,
                bean.getClass().getName()
        );
    }

    private void unregister(String pluginId, RequestMappingHandlerMapping mapping, List<RequestMappingInfo> registered) {
        for (RequestMappingInfo info : registered) {
            try {
                mapping.unregisterMapping(info);
            } catch (RuntimeException e) {
                log.warn("Failed to unregister web endpoint {} from plugin '{}'", info, pluginId, e);
            }
        }
    }

    private RequestMappingHandlerMapping resolveHandlerMapping() {
        try {
            return rootContext.getBean(HANDLER_MAPPING_BEAN_NAME, RequestMappingHandlerMapping.class);
        } catch (BeansException byName) {
            try {
                return rootContext.getBeanProvider(RequestMappingHandlerMapping.class).getIfUnique();
            } catch (BeansException byType) {
                return null;
            }
        }
    }

    private boolean isController(Class<?> type) {
        return AnnotatedElementUtils.hasAnnotation(type, Controller.class);
    }

    private static Method resolveDetectHandlerMethods() {
        try {
            Method method = AbstractHandlerMethodMapping.class.getDeclaredMethod("detectHandlerMethods", Object.class);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(
                    "Spring MVC AbstractHandlerMethodMapping.detectHandlerMethods(Object) is unavailable", e);
        }
    }
}
