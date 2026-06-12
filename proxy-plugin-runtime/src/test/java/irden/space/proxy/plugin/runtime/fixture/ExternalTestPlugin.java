package irden.space.proxy.plugin.runtime.fixture;

import irden.space.proxy.plugin.api.PluginContext;
import irden.space.proxy.plugin.api.PluginDefinition;
import irden.space.proxy.plugin.api.ProxyPlugin;

@PluginDefinition(id = "external-test", name = "External Test", version = "1.0.0")
public final class ExternalTestPlugin implements ProxyPlugin {

    public ExternalTestPlugin(PluginContext pluginContext) {
    }
}
