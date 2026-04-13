package irden.space.proxy.plugin.api;

public interface PluginAnnotationRegistrar {

    boolean supports(Class<?> pluginType);

    void register(ProxyPlugin plugin, PluginContext context);
}
