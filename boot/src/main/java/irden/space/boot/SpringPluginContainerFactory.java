package irden.space.boot;

import irden.space.proxy.plugin.api.*;
import irden.space.proxy.plugin.api.annotations.PacketHandler;
import irden.space.proxy.plugin.api.annotations.RegisterPluginPermissions;
import irden.space.boot.security.PluginMethodSecurityConfiguration;
import irden.space.proxy.plugin.runtime.*;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindingPostProcessor;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.util.*;

public final class SpringPluginContainerFactory implements PluginContainerFactory {

    private static final String PLUGIN_CONTEXT_BEAN_NAME = "pluginContext";
    private static final String PLUGIN_PACKET_INTERCEPTOR_REGISTRY_BEAN_NAME = "pluginPacketInterceptorRegistry";
    private static final String MESSAGE_SOURCE_BEAN_NAME = "messageSource";

    private final ApplicationContext rootContext;
    private final PluginWebEndpointRegistrar webEndpointRegistrar;
    private final PluginEventListenerRegistrar eventListenerRegistrar;
    private final PluginConfigInitializer configInitializer;

    public SpringPluginContainerFactory(ApplicationContext rootContext) {
        this.rootContext = Objects.requireNonNull(rootContext, "rootContext");
        this.webEndpointRegistrar = new PluginWebEndpointRegistrar(this.rootContext);
        this.eventListenerRegistrar = new PluginEventListenerRegistrar(this.rootContext);
        this.configInitializer = new PluginConfigInitializer(this.rootContext);
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
            configInitializer.apply(pluginContext, candidate);
            ConfigurationPropertiesBindingPostProcessor.register(pluginContext);
            pluginContext.register(PluginMethodSecurityConfiguration.class);
            registerScopedRuntimeBeans(pluginContext, scopedContext);
            registerDependencyBeans(pluginContext, dependencyContainers);
            registerPluginMessageSource(pluginContext, candidate);

            PluginSpringConfiguration configuration = candidate.pluginClass().getAnnotation(PluginSpringConfiguration.class);
            if (configuration == null || configuration.scanPluginPackage()) {
                scanPluginComponents(pluginContext, candidate.pluginClass());
            }
            if (configuration != null && configuration.value().length > 0) {
                pluginContext.register(configuration.value());
            }

            registerPluginBean(pluginContext, candidate);
            disableNativeEventListenerProcessing(pluginContext);
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
                    SpringPluginContainerFactory.this.registerPluginPermissions(
                            candidate.pluginClass(), pluginContext, scopedContext);
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

    private void registerPluginMessageSource(
            AnnotationConfigApplicationContext context,
            PluginCandidate candidate
    ) {
        String basename = "i18n/" + candidate.descriptor().id() + "/messages";
        ClassLoader pluginClassLoader = candidate.pluginClass().getClassLoader();
        context.registerBean(
                MESSAGE_SOURCE_BEAN_NAME,
                ResourceBundleMessageSource.class,
                () -> {
                    ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
                    messageSource.setBasename(basename);
                    messageSource.setDefaultEncoding("UTF-8");
                    messageSource.setBundleClassLoader(pluginClassLoader);
                    messageSource.setFallbackToSystemLocale(false);
                    return messageSource;
                },
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

        Map<String, Object> localBeans = localApplicationBeans(context);
        localBeans.forEach((_, bean) -> {
            if (hasPacketHandlers(bean.getClass())) {
                scopedContext.packetInterceptorRegistry().registerAnnotated(bean);
            }
            for (PluginAnnotationRegistrar registrar : registrars) {
                if (registrar.supports(bean.getClass())) {
                    registrar.register(bean, owner, scopedContext);
                }
            }
        });

        webEndpointRegistrar.registerControllers(owner.id(), localBeans.values(), scopedContext);
        eventListenerRegistrar.registerListeners(owner.id(), localBeans.values(), scopedContext);
    }

    private void registerPluginPermissions(
            Class<? extends ProxyPlugin> pluginClass,
            AnnotationConfigApplicationContext context,
            PluginContext scopedContext
    ) {
        if (scopedContext == null) {
            return;
        }
        localApplicationBeans(context).values()
                .forEach(bean -> PluginPermissionRegistrar.register(bean, scopedContext));
        registerAnnotatedPermissionEnums(pluginClass, context);
    }

    /**
     * Scans the plugin package for {@link PermissionEnum} enums annotated with
     * {@link RegisterPluginPermissions} and registers their default nodes. Enums are not Spring
     * beans, so they cannot be discovered through the bean-iteration path above.
     */
    private void registerAnnotatedPermissionEnums(
            Class<? extends ProxyPlugin> pluginClass,
            AnnotationConfigApplicationContext context
    ) {
        ClassLoader classLoader = context.getClassLoader();
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false) {
                    @Override
                    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                        return beanDefinition.getMetadata().isIndependent();
                    }
                };
        scanner.setResourceLoader(new PathMatchingResourcePatternResolver(classLoader));
        scanner.addIncludeFilter(new AnnotationTypeFilter(RegisterPluginPermissions.class));

        for (BeanDefinition candidate : scanner.findCandidateComponents(pluginClass.getPackageName())) {
            PermissionEnum.registerDefaults(loadPermissionEnum(candidate.getBeanClassName(), classLoader));
        }
    }

    private Class<? extends PermissionEnum> loadPermissionEnum(String className, ClassLoader classLoader) {
        Class<?> type;
        try {
            type = Class.forName(className, false, classLoader);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Failed to load @RegisterPluginPermissions type " + className, e);
        }
        if (!type.isEnum() || !PermissionEnum.class.isAssignableFrom(type)) {
            throw new IllegalStateException(
                    "@RegisterPluginPermissions on a type is only supported for PermissionEnum enums: " + className);
        }
        return type.asSubclass(PermissionEnum.class);
    }

    private void onLoad(
            AnnotationConfigApplicationContext context,
            PluginContext scopedContext
    ) {
        if (scopedContext == null) {
            return;
        }
        localApplicationBeans(context).values()
                .forEach(bean -> PluginLifecycleInvoker.onLoad(bean, scopedContext));
    }

    private void onStart(AnnotationConfigApplicationContext context) {
        localApplicationBeans(context).values()
                .forEach(PluginLifecycleInvoker::onStart);
    }

    private void onConnectionSuccess(
            AnnotationConfigApplicationContext context,
            PluginSessionContext sessionContext
    ) {
        localApplicationBeans(context).values()
                .forEach(bean -> PluginLifecycleInvoker.onConnectionSuccess(bean, sessionContext));
    }

    private void onDisconnecting(
            AnnotationConfigApplicationContext context,
            PluginSessionContext sessionContext
    ) {
        List<Object> beans = new java.util.ArrayList<>(localApplicationBeans(context).values());
        java.util.Collections.reverse(beans);
        beans.forEach(bean -> PluginLifecycleInvoker.onDisconnecting(bean, sessionContext));
    }

    private void onDisconnected(
            AnnotationConfigApplicationContext context,
            PluginSessionContext sessionContext
    ) {
        List<Object> beans = new java.util.ArrayList<>(localApplicationBeans(context).values());
        java.util.Collections.reverse(beans);
        beans.forEach(bean -> PluginLifecycleInvoker.onDisconnected(bean, sessionContext));
    }

    private void onStop(AnnotationConfigApplicationContext context) {
        List<Object> beans = new java.util.ArrayList<>(localApplicationBeans(context).values());
        java.util.Collections.reverse(beans);
        beans.forEach(PluginLifecycleInvoker::onStop);
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
                || MESSAGE_SOURCE_BEAN_NAME.equals(beanName)
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

    /**
     * Plugin {@code @EventListener} methods are bridged onto the root application event multicaster
     * (see {@link PluginEventListenerRegistrar}) so events published from one plugin reach listeners
     * in others. To keep delivery exactly-once, the child context must not <em>also</em> register the
     * same listeners on its own multicaster - otherwise an intra-plugin event (published and observed
     * within the same plugin) would fire twice: once locally and once after bubbling up to the root.
     */
    private void disableNativeEventListenerProcessing(AnnotationConfigApplicationContext context) {
        String processorBeanName = org.springframework.context.annotation.AnnotationConfigUtils
                .EVENT_LISTENER_PROCESSOR_BEAN_NAME;
        if (context.getBeanFactory().containsBeanDefinition(processorBeanName)) {
            context.getDefaultListableBeanFactory().removeBeanDefinition(processorBeanName);
        }
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
