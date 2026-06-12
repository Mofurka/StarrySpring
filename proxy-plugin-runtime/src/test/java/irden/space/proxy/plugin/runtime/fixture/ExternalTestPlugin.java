package irden.space.proxy.plugin.runtime.fixture;

import irden.space.proxy.plugin.api.PluginDescriptor;
import irden.space.proxy.plugin.api.ProxyPlugin;

import java.util.List;

public final class ExternalTestPlugin implements ProxyPlugin {

    @Override
    public PluginDescriptor descriptor() {
        return new PluginDescriptor("external-test", "External Test", "1.0.0", List.of());
    }
}
