package irden.space.proxy.plugin.runtime;

import irden.space.proxy.plugin.api.PluginContext;

public interface PluginContextManager extends PluginContext {

    PluginContext forPlugin(String pluginId);

    void removePlugin(String pluginId);
}
