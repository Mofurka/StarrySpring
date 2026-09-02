package irden.space.proxy.plugin.osb_detector;

import irden.space.proxy.plugin.api.PluginDefinition;
import irden.space.proxy.plugin.api.ProxyPlugin;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@PluginDefinition(
        id = "osb-detector",
        name = "osb-detector",
        version = "1.0.0",
        author = "https://github.com/Mofurka",
        description = ""
)
@Component
@EnableConfigurationProperties(OsbDetectorConfiguration.class)
public final class OsbDetectorPlugin implements ProxyPlugin {

}
