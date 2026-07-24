package irden.space.proxy.plugin.irden;

import irden.space.proxy.plugin.api.PluginDefinition;
import irden.space.proxy.plugin.api.ProxyPlugin;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@PluginDefinition(
        id = "irden",
        name = "Irden Plugin",
        version = "1.0.0",
        author = "https://github.com/Mofurka",
        dependsOn = {"command-handler", "player-manager"},
        description = ""
)
@Component
@EnableConfigurationProperties({IrdenConfig.class})
public final class IrdenPlugin implements ProxyPlugin {

}
