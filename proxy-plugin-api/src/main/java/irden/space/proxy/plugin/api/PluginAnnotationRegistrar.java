package irden.space.proxy.plugin.api;

public interface PluginAnnotationRegistrar {

    boolean supports(Class<?> beanType);

    void register(Object bean, PluginDescriptor owner, PluginContext context);
}
