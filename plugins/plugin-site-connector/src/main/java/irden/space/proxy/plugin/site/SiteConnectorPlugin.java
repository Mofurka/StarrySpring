package irden.space.proxy.plugin.site;

import irden.space.proxy.plugin.api.PluginDefinition;
import irden.space.proxy.plugin.api.ProxyPlugin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@PluginDefinition(
        id = "site-connector",
        name = "Site Connector",
        version = "1.0.0",
        dependsOn = {"player-manager", "general", "irden"},
        author = "https://github.com/Mofurka",
        description = "Integration plugin between app.irden.space and the game server"
)
@Component

@Slf4j
@RequiredArgsConstructor
public final class SiteConnectorPlugin implements ProxyPlugin {
}
