package irden.space.proxy.plugin.site;

import irden.space.proxy.plugin.api.PluginDefinition;
import irden.space.proxy.plugin.api.ProxyPlugin;
import org.springframework.stereotype.Component;

@PluginDefinition(
        id = "site-connector",
        name = "Site Connector",
        version = "1.0.0",
        dependsOn = {"player-manager", "general"},
        author = "https://github.com/Mofurka",
        description = "Integration plugin between app.irden.space and the game server"
)
@Component
public final class SiteConnectorPlugin implements ProxyPlugin {

}
