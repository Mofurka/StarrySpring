package irden.space.proxy.plugin.general;

import irden.space.proxy.plugin.api.PluginDefinition;
import irden.space.proxy.plugin.api.ProxyPlugin;
import org.springframework.stereotype.Component;

@PluginDefinition(
        id = "general",
        name = "General Plugin",
        version = "1.0.0",
        dependsOn = {"command-handler", "player-manager"},
        author = "https://github.com/Mofurka",
        description = "A plugin for enhance chat functionality and it manage and other things."
)
@Component
public final class GeneralPlugin implements ProxyPlugin {

}
