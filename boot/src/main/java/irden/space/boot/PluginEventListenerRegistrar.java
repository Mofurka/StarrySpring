package irden.space.boot;

import irden.space.proxy.plugin.api.PluginContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.EventListener;
import org.springframework.context.event.GenericApplicationListener;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;


final class PluginEventListenerRegistrar {

    private static final Logger log = LoggerFactory.getLogger(PluginEventListenerRegistrar.class);

    private final ApplicationContext rootContext;

    PluginEventListenerRegistrar(ApplicationContext rootContext) {
        this.rootContext = Objects.requireNonNull(rootContext, "rootContext");
    }

    void registerListeners(String pluginId, Collection<Object> beans, PluginContext scopedContext) {
        ApplicationEventMulticaster multicaster = resolveMulticaster();
        if (multicaster == null) {
            return;
        }

        for (Object bean : beans) {
            registerBean(pluginId, multicaster, bean, scopedContext);
        }
    }

    private void registerBean(
            String pluginId,
            ApplicationEventMulticaster multicaster,
            Object bean,
            PluginContext scopedContext
    ) {
        Class<?> targetType = AopUtils.getTargetClass(bean);
        Map<Method, EventListener> annotatedMethods = MethodIntrospector.selectMethods(
                targetType,
                (MethodIntrospector.MetadataLookup<EventListener>) method ->
                        AnnotatedElementUtils.findMergedAnnotation(method, EventListener.class)
        );

        annotatedMethods.forEach((method, annotation) -> {
            Class<?> eventType = resolveEventType(method);
            if (eventType == null) {
                log.warn(
                        "Skipping @EventListener {}#{} from plugin '{}': cannot infer event type "
                                + "(declare it via the method parameter)",
                        targetType.getName(), method.getName(), pluginId
                );
                return;
            }
            PluginMethodEventListener listener = new PluginMethodEventListener(bean, method, eventType);
            multicaster.addApplicationListener(listener);
            scopedContext.onRemove(() -> multicaster.removeApplicationListener(listener));
            log.info(
                    "Registered @EventListener from plugin '{}' for {} -> {}#{}",
                    pluginId, eventType.getName(), targetType.getSimpleName(), method.getName()
            );
        });
    }

    private Class<?> resolveEventType(Method method) {
        if (method.getParameterCount() == 1) {
            return method.getParameterTypes()[0];
        }
        return null;
    }

    private ApplicationEventMulticaster resolveMulticaster() {
        try {
            return rootContext.getBean(
                    AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME,
                    ApplicationEventMulticaster.class
            );
        } catch (BeansException e) {
            log.warn("No application event multicaster available; plugin @EventListener bridging disabled", e);
            return null;
        }
    }


    private static final class PluginMethodEventListener implements GenericApplicationListener {

        private final Object bean;
        private final Method method;
        private final Class<?> eventType;

        private PluginMethodEventListener(Object bean, Method method, Class<?> eventType) {
            this.bean = bean;
            this.method = method;
            this.eventType = eventType;
            ReflectionUtils.makeAccessible(method);
        }

        @Override
        public boolean supportsEventType(ResolvableType type) {
            Class<?> raw = type.toClass();
            if (eventType.isAssignableFrom(raw)) {
                return true;
            }
            if (PayloadApplicationEvent.class.isAssignableFrom(raw)) {
                ResolvableType payload = type.as(PayloadApplicationEvent.class).getGeneric();
                Class<?> payloadType = payload.resolve();
                return payloadType != null && eventType.isAssignableFrom(payloadType);
            }
            return false;
        }

        @Override
        public void onApplicationEvent(ApplicationEvent event) {
            Object payload = event instanceof PayloadApplicationEvent<?> wrapper
                    ? wrapper.getPayload()
                    : event;
            if (!eventType.isInstance(payload)) {
                return;
            }
            ReflectionUtils.invokeMethod(method, bean, payload);
        }
    }
}
