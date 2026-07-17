package irden.space.boot.pluginfixture;

import irden.space.proxy.plugin.api.PluginDescriptor;
import irden.space.proxy.plugin.api.ProxyPlugin;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationPropertiesScan
public final class ConfigScanPlugin implements ProxyPlugin {

    @Override
    public PluginDescriptor descriptor() {
        return new PluginDescriptor("config-scan", "Config Scan", "1.0.0", List.of());
    }
}
