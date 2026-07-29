package irden.space.boot;

import irden.space.proxy.plugin.api.PluginContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.method.ControllerAdviceBean;
import org.springframework.web.method.annotation.ExceptionHandlerMethodResolver;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.handler.HandlerExceptionResolverComposite;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

final class PluginControllerAdviceRegistrar {

    private static final Logger log = LoggerFactory.getLogger(PluginControllerAdviceRegistrar.class);
    private static final Field ADVICE_CACHE_FIELD = resolveAdviceCacheField();

    private final ApplicationContext rootContext;

    PluginControllerAdviceRegistrar(ApplicationContext rootContext) {
        this.rootContext = Objects.requireNonNull(rootContext, "rootContext");
    }

    private static Field resolveAdviceCacheField() {
        try {
            Field field = ExceptionHandlerExceptionResolver.class.getDeclaredField("exceptionHandlerAdviceCache");
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(
                    "Spring MVC ExceptionHandlerExceptionResolver.exceptionHandlerAdviceCache is unavailable", e);
        }
    }

    void registerAdvices(
            String pluginId,
            BeanFactory pluginBeanFactory,
            Map<String, Object> localBeans,
            PluginContext scopedContext
    ) {
        ExceptionHandlerExceptionResolver resolver = resolveResolver();
        if (resolver == null) {
            return;
        }

        localBeans.forEach((beanName, bean) -> {
            if (isControllerAdvice(bean.getClass())) {
                registerAdvice(pluginId, resolver, pluginBeanFactory, beanName, bean, scopedContext);
            }
        });
    }

    private void registerAdvice(
            String pluginId,
            ExceptionHandlerExceptionResolver resolver,
            BeanFactory pluginBeanFactory,
            String beanName,
            Object bean,
            PluginContext scopedContext
    ) {
        ControllerAdvice annotation = AnnotatedElementUtils.findMergedAnnotation(bean.getClass(), ControllerAdvice.class);
        if (annotation == null) {
            return;
        }

        ExceptionHandlerMethodResolver methodResolver = new ExceptionHandlerMethodResolver(bean.getClass());
        if (!methodResolver.hasExceptionMappings()) {
            return;
        }

        ControllerAdviceBean adviceBean = new ControllerAdviceBean(beanName, pluginBeanFactory, annotation);

        synchronized (this) {
            Map<ControllerAdviceBean, ExceptionHandlerMethodResolver> current = readCache(resolver);
            Map<ControllerAdviceBean, ExceptionHandlerMethodResolver> updated = new LinkedHashMap<>(current);
            updated.put(adviceBean, methodResolver);
            writeCache(resolver, updated);
        }

        scopedContext.onRemove(() -> unregister(pluginId, resolver, adviceBean));
        log.info(
                "Registered @ControllerAdvice '{}' from plugin '{}' with the root exception resolver",
                bean.getClass().getName(),
                pluginId
        );
    }

    private void unregister(String pluginId, ExceptionHandlerExceptionResolver resolver, ControllerAdviceBean adviceBean) {
        synchronized (this) {
            Map<ControllerAdviceBean, ExceptionHandlerMethodResolver> current = readCache(resolver);
            if (!current.containsKey(adviceBean)) {
                return;
            }
            Map<ControllerAdviceBean, ExceptionHandlerMethodResolver> updated = new LinkedHashMap<>(current);
            updated.remove(adviceBean);
            writeCache(resolver, updated);
        }
        log.info("Unregistered a @ControllerAdvice from plugin '{}' from the root exception resolver", pluginId);
    }

    private ExceptionHandlerExceptionResolver resolveResolver() {
        try {
            ExceptionHandlerExceptionResolver direct =
                    rootContext.getBeanProvider(ExceptionHandlerExceptionResolver.class).getIfAvailable();
            if (direct != null) {
                return direct;
            }
            for (HandlerExceptionResolver resolver : rootContext.getBeansOfType(HandlerExceptionResolver.class).values()) {
                ExceptionHandlerExceptionResolver found = unwrap(resolver);
                if (found != null) {
                    return found;
                }
            }
            return null;
        } catch (BeansException e) {
            return null;
        }
    }

    private ExceptionHandlerExceptionResolver unwrap(HandlerExceptionResolver resolver) {
        if (resolver instanceof ExceptionHandlerExceptionResolver exceptionHandlerResolver) {
            return exceptionHandlerResolver;
        }
        if (resolver instanceof HandlerExceptionResolverComposite composite) {
            for (HandlerExceptionResolver nested : composite.getExceptionResolvers()) {
                ExceptionHandlerExceptionResolver found = unwrap(nested);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private boolean isControllerAdvice(Class<?> type) {
        return AnnotatedElementUtils.hasAnnotation(type, ControllerAdvice.class);
    }

    @SuppressWarnings("unchecked")
    private Map<ControllerAdviceBean, ExceptionHandlerMethodResolver> readCache(ExceptionHandlerExceptionResolver resolver) {
        try {
            return (Map<ControllerAdviceBean, ExceptionHandlerMethodResolver>) ADVICE_CACHE_FIELD.get(resolver);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to read exceptionHandlerAdviceCache", e);
        }
    }

    private void writeCache(ExceptionHandlerExceptionResolver resolver, Map<ControllerAdviceBean, ExceptionHandlerMethodResolver> cache) {
        try {
            ADVICE_CACHE_FIELD.set(resolver, cache);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to write exceptionHandlerAdviceCache", e);
        }
    }
}
