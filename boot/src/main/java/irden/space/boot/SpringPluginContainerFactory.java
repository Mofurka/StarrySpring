package irden.space.boot;

import irden.space.proxy.plugin.api.PacketInterceptorRegistry;
import irden.space.proxy.plugin.api.PluginContext;
import irden.space.proxy.plugin.api.PluginSpringConfiguration;
import irden.space.proxy.plugin.api.ProxyPlugin;
import irden.space.proxy.plugin.runtime.PluginCandidate;
import irden.space.proxy.plugin.runtime.PluginContainer;
import irden.space.proxy.plugin.runtime.PluginContainerFactory;
import irden.space.proxy.plugin.runtime.PluginServiceProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.util.Objects;

public final class SpringPluginContainerFactory implements PluginContainerFactory {

    private final ApplicationContext rootContext;
    private final PluginServiceProvider pluginServiceProvider;

    public SpringPluginContainerFactory(ApplicationContext rootContext) {
        this(rootContext, pluginIds -> java.util.Map.of());
    }

    public SpringPluginContainerFactory(ApplicationContext rootContext, PluginServiceProvider pluginServiceProvider) {
        this.rootContext = Objects.requireNonNull(rootContext, "rootContext");
        this.pluginServiceProvider = Objects.requireNonNull(pluginServiceProvider, "pluginServiceProvider");
    }

    @Override
    public PluginContainer create(ProxyPlugin plugin) {
        return create(plugin, null);
    }

    @Override
    public PluginContainer create(ProxyPlugin plugin, PluginContext scopedContext) {
        return create(PluginCandidate.fromInstance(plugin), scopedContext);
    }

    @Override
    public PluginContainer create(PluginCandidate candidate, PluginContext scopedContext) {
        AnnotationConfigApplicationContext pluginContext = new AnnotationConfigApplicationContext();
        try {
            pluginContext.setParent(rootContext);
            pluginContext.setClassLoader(candidate.pluginClass().getClassLoader());
            registerScopedRuntimeBeans(pluginContext, scopedContext);
            registerDependencyServices(pluginContext, candidate);

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
                "pluginContext",
                PluginContext.class,
                () -> scopedContext,
                beanDefinition -> beanDefinition.setPrimary(true)
        );
        context.registerBean(
                "pluginPacketInterceptorRegistry",
                PacketInterceptorRegistry.class,
                scopedContext::packetInterceptorRegistry,
                beanDefinition -> beanDefinition.setPrimary(true)
        );
    }

    private void registerDependencyServices(AnnotationConfigApplicationContext context, PluginCandidate candidate) {
        pluginServiceProvider.servicesPublishedBy(candidate.descriptor().dependsOn())
                .forEach((serviceType, service) -> context.getBeanFactory().registerSingleton(
                        dependencyServiceBeanName(serviceType),
                        service
                ));
    }

    private String dependencyServiceBeanName(Class<?> serviceType) {
        return "pluginService:" + serviceType.getName();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void registerPluginBean(AnnotationConfigApplicationContext context, PluginCandidate candidate) {
        if (candidate.bootstrapInstance() == null) {
            context.registerBean("proxyPlugin", (Class) candidate.pluginClass());
            return;
        }
        context.registerBean(
                "proxyPlugin",
                (Class) candidate.pluginClass(),
                candidate::bootstrapInstance
        );
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
