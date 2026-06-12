package irden.space.boot.pluginfixture;

import irden.space.proxy.plugin.api.PluginDescriptor;
import irden.space.proxy.plugin.api.ProxyPlugin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public final class ComponentScannedPlugin implements ProxyPlugin {

    @Autowired
    private ScannedDependency dependency;

    public ScannedDependency dependency() {
        return dependency;
    }

    @Override
    public PluginDescriptor descriptor() {
        return new PluginDescriptor("component-scanned", "Component Scanned", "1.0.0", List.of());
    }
}
