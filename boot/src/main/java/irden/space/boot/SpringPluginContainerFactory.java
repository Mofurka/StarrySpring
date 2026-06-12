package irden.space.boot;

import irden.space.proxy.plugin.api.PluginSpringConfiguration;
import irden.space.proxy.plugin.api.ProxyPlugin;
import irden.space.proxy.plugin.runtime.PluginContainer;
import irden.space.proxy.plugin.runtime.PluginContainerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.util.Objects;

public final class SpringPluginContainerFactory implements PluginContainerFactory {

    private final ApplicationContext rootContext;

    public SpringPluginContainerFactory(ApplicationContext rootContext) {
        this.rootContext = Objects.requireNonNull(rootContext, "rootContext");
    }

    @Override
    public PluginContainer create(ProxyPlugin plugin) {
        AnnotationConfigApplicationContext pluginContext = new AnnotationConfigApplicationContext();
        try {
            pluginContext.setParent(rootContext);
            pluginContext.setClassLoader(plugin.getClass().getClassLoader());

            PluginSpringConfiguration configuration = plugin.getClass().getAnnotation(PluginSpringConfiguration.class);
            if (configuration == null || configuration.scanPluginPackage()) {
                scanPluginComponents(pluginContext, plugin);
            }
            if (configuration != null && configuration.value().length > 0) {
                pluginContext.register(configuration.value());
            }

            registerPluginBean(pluginContext, plugin);
            pluginContext.refresh();

            return new PluginContainer() {
                @Override
                public ProxyPlugin plugin() {
                    return pluginContext.getBean(plugin.getClass());
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

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void registerPluginBean(AnnotationConfigApplicationContext context, ProxyPlugin plugin) {
        context.registerBean("proxyPlugin", (Class) plugin.getClass(), () -> plugin);
    }

    private void scanPluginComponents(AnnotationConfigApplicationContext context, ProxyPlugin plugin) {
        ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
        scanner.addExcludeFilter(new AssignableTypeFilter(ProxyPlugin.class));
        scanner.scan(plugin.getClass().getPackageName());
    }
}
