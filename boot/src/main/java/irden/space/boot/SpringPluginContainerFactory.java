package irden.space.boot;

import irden.space.proxy.plugin.api.PacketInterceptorRegistry;
import irden.space.proxy.plugin.api.PluginContext;
import irden.space.proxy.plugin.api.PluginSpringConfiguration;
import irden.space.proxy.plugin.api.ProxyPlugin;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
