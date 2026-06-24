package irden.space.boot;

import irden.space.proxy.plugin.api.*;
import irden.space.proxy.plugin.api.annotations.PacketHandler;
import irden.space.proxy.plugin.runtime.PluginCandidate;
import irden.space.proxy.plugin.runtime.PluginContainer;
import irden.space.proxy.plugin.runtime.PluginContainerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.util.*;

public final class SpringPluginContainerFactory implements PluginContainerFactory {

    private static final String PLUGIN_CONTEXT_BEAN_NAME = "pluginContext";
    private static final String PLUGIN_PACKET_INTERCEPTOR_REGISTRY_BEAN_NAME = "pluginPacketInterceptorRegistry";

    private final ApplicationContext rootContext;

    public SpringPluginContainerFactory(ApplicationContext rootContext) {
        this.rootContext = Objects.requireNonNull(rootContext, "rootContext");
    }

    @Override
    public PluginContainer create(
            PluginCandidate candidate,
            PluginContext scopedContext,
            List<PluginContainer> dependencyContainers
    ) {
        AnnotationConfigApplicationContext pluginContext = new AnnotationConfigApplicationContext();
        try {
            pluginContext.setParent(rootContext);
            pluginContext.setClassLoader(candidate.pluginClass().getClassLoader());
            registerScopedRuntimeBeans(pluginContext, scopedContext);
            registerDependencyBeans(pluginContext, dependencyContainers);

            PluginSpringConfiguration configuration = candidate.pluginClass().getAnnotation(PluginSpringConfiguration.class);
            if (configuration == null || configuration.scanPluginPackage()) {
                scanPluginComponents(pluginContext, candidate.pluginClass());
            }
            if (configuration != null && configuration.value().length > 0) {
                pluginContext.register(configuration.value());
            }

            registerPluginBean(pluginContext, candidate);
            pluginContext.refresh();

            return new PluginContainer() {
                @Override
                public ProxyPlugin plugin() {
                    return pluginContext.getBean(candidate.pluginClass());
                }

                @Override
                public <T> Map<String, T> beansOfType(Class<T> type) {
                    return exportableBeansOfType(pluginContext, type);
                }

                @Override
                public void registerAnnotatedBeans() {
                    SpringPluginContainerFactory.this.registerAnnotatedBeans(
                            pluginContext,
                            candidate.descriptor(),
                            scopedContext
                    );
                }

                @Override
                public void registerPluginPermissions() {
                    SpringPluginContainerFactory.this.registerPluginPermissions(pluginContext, scopedContext);
                }

                @Override
                public void onLoad() {
                    SpringPluginContainerFactory.this.onLoad(pluginContext, scopedContext);
                }

                @Override
                public void onStart() {
                    SpringPluginContainerFactory.this.onStart(pluginContext);
                }

                @Override
                public void onConnectionSuccess(PluginSessionContext context) {
                    SpringPluginContainerFactory.this.onConnectionSuccess(pluginContext, context);
                }

                @Override
                public void onDisconnecting(PluginSessionContext context) {
                    SpringPluginContainerFactory.this.onDisconnecting(pluginContext, context);
                }

                @Override
                public void onDisconnected(PluginSessionContext context) {
                    SpringPluginContainerFactory.this.onDisconnected(pluginContext, context);
                }

                @Override
                public void onStop() {
                    SpringPluginContainerFactory.this.onStop(pluginContext);
                }

                @Override
                public void close() {
                    pluginContext.close();
                }
            };
        } catch (RuntimeException | Error e) {
            pluginContext.close();
            throw e;
        }
    }

    private void registerScopedRuntimeBeans(
            AnnotationConfigApplicationContext context,
            PluginContext scopedContext
    ) {
        if (scopedContext == null) {
            return;
        }
        context.registerBean(
                PLUGIN_CONTEXT_BEAN_NAME,
                PluginContext.class,
                () -> scopedContext,
                beanDefinition -> beanDefinition.setPrimary(true)
        );
        context.registerBean(
                PLUGIN_PACKET_INTERCEPTOR_REGISTRY_BEAN_NAME,
                PacketInterceptorRegistry.class,
                scopedContext::packetInterceptorRegistry,
                beanDefinition -> beanDefinition.setPrimary(true)
        );
    }

    private void registerDependencyBeans(
            AnnotationConfigApplicationContext context,
            List<PluginContainer> dependencyContainers
    ) {
        if (dependencyContainers == null || dependencyContainers.isEmpty()) {
            return;
        }

        for (PluginContainer dependencyContainer : dependencyContainers) {
            String dependencyPluginId = dependencyContainer.plugin().descriptor().id();
            dependencyContainer.beansOfType(Object.class)
                    .forEach((beanName, bean) -> context.getBeanFactory().registerSingleton(
                            dependencyBeanName(dependencyPluginId, beanName),
                            bean
                    ));
        }
    }

    private String dependencyBeanName(String pluginId, String beanName) {
        return "pluginDependency:%s:%s".formatted(pluginId, beanName);
    }

    private <T> Map<String, T> exportableBeansOfType(
            AnnotationConfigApplicationContext context,
            Class<T> type
    ) {
        Map<String, T> beans = context.getBeansOfType(type, false, false);
        Map<String, T> exportableBeans = new LinkedHashMap<>();
        beans.forEach((beanName, bean) -> {
            if (isExportableBean(context, beanName, bean)) {
                exportableBeans.put(beanName, bean);
            }
        });
        return Map.copyOf(exportableBeans);
    }

    private void registerAnnotatedBeans(
            AnnotationConfigApplicationContext context,
            irden.space.proxy.plugin.api.PluginDescriptor owner,
            PluginContext scopedContext
    ) {
        if (scopedContext == null) {
            return;
        }

        List<PluginAnnotationRegistrar> registrars = ServiceLoader.load(
                        PluginAnnotationRegistrar.class,
                        context.getClassLoader()
                )
                .stream()
                .map(ServiceLoader.Provider::get)
                .toList();

        localApplicationBeans(context).forEach((beanName, bean) -> {
            if (hasPacketHandlers(bean.getClass())) {
                scopedContext.packetInterceptorRegistry().registerAnnotated(bean);
            }
            for (PluginAnnotationRegistrar registrar : registrars) {
                if (registrar.supports(bean.getClass())) {
                    registrar.register(bean, owner, scopedContext);
                }
            }
        });
    }

    private void registerPluginPermissions(
            AnnotationConfigApplicationContext context,
            PluginContext scopedContext
    ) {
        if (scopedContext == null) {
            return;
        }
        localApplicationBeans(context).values()
                .forEach(bean -> ProxyPluginSupport.registerPluginPermissions(bean, scopedContext));
    }

    private void onLoad(
            AnnotationConfigApplicationContext context,
            PluginContext scopedContext
    ) {
        if (scopedContext == null) {
            return;
        }
        localApplicationBeans(context).values()
                .forEach(bean -> ProxyPluginSupport.onLoad(bean, scopedContext));
    }

    private void onStart(AnnotationConfigApplicationContext context) {
        localApplicationBeans(context).values()
                .forEach(ProxyPluginSupport::onStart);
    }

    private void onConnectionSuccess(
            AnnotationConfigApplicationContext context,
            PluginSessionContext sessionContext
    ) {
        localApplicationBeans(context).values()
                .forEach(bean -> ProxyPluginSupport.onConnectionSuccess(bean, sessionContext));
    }

    private void onDisconnecting(
            AnnotationConfigApplicationContext context,
            PluginSessionContext sessionContext
    ) {
        List<Object> beans = new java.util.ArrayList<>(localApplicationBeans(context).values());
        java.util.Collections.reverse(beans);
        beans.forEach(bean -> ProxyPluginSupport.onDisconnecting(bean, sessionContext));
    }

    private void onDisconnected(
            AnnotationConfigApplicationContext context,
            PluginSessionContext sessionContext
    ) {
        List<Object> beans = new java.util.ArrayList<>(localApplicationBeans(context).values());
        java.util.Collections.reverse(beans);
        beans.forEach(bean -> ProxyPluginSupport.onDisconnected(bean, sessionContext));
    }

    private void onStop(AnnotationConfigApplicationContext context) {
        List<Object> beans = new java.util.ArrayList<>(localApplicationBeans(context).values());
        java.util.Collections.reverse(beans);
        beans.forEach(ProxyPluginSupport::onStop);
    }

    private Map<String, Object> localApplicationBeans(AnnotationConfigApplicationContext context) {
        Map<String, Object> result = new LinkedHashMap<>();
        context.getBeansOfType(Object.class, false, false)
                .forEach((beanName, bean) -> {
                    if (isLocalApplicationBean(context, beanName, bean)) {
                        result.put(beanName, bean);
                    }
                });
        return Map.copyOf(result);
    }

    private boolean hasPacketHandlers(Class<?> beanType) {
        return java.util.Arrays.stream(beanType.getDeclaredMethods())
                .anyMatch(method -> method.isAnnotationPresent(PacketHandler.class));
    }

    private boolean isExportableBean(
            AnnotationConfigApplicationContext context,
            String beanName,
            Object bean
    ) {
        if (PLUGIN_CONTEXT_BEAN_NAME.equals(beanName)
                || PLUGIN_PACKET_INTERCEPTOR_REGISTRY_BEAN_NAME.equals(beanName)
                || beanName.startsWith("org.springframework.")) {
            return false;
        }
        if (bean instanceof BeanFactoryPostProcessor || bean instanceof BeanPostProcessor) {
            return false;
        }
        if (!context.containsBeanDefinition(beanName)) {
            return false;
        }
        return context.getBeanDefinition(beanName).getRole() == BeanDefinition.ROLE_APPLICATION;
    }

    private boolean isLocalApplicationBean(
            AnnotationConfigApplicationContext context,
            String beanName,
            Object bean
    ) {
        return !beanName.startsWith("pluginDependency:")
                && isExportableBean(context, beanName, bean);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void registerPluginBean(AnnotationConfigApplicationContext context, PluginCandidate candidate) {
        context.registerBean("proxyPlugin", (Class) candidate.pluginClass());
    }

    private void scanPluginComponents(
            AnnotationConfigApplicationContext context,
            Class<? extends ProxyPlugin> pluginClass
    ) {
        ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
        scanner.addExcludeFilter(new AssignableTypeFilter(ProxyPlugin.class));
        scanner.scan(pluginClass.getPackageName());
    }
}
