package irden.space.proxy.plugin.planet_protect;

import irden.space.proxy.plugin.api.PluginDefinition;
import irden.space.proxy.plugin.api.ProxyPlugin;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@PluginDefinition(
        id = "planet-protect",
        name = "planet-protect",
        version = "1.0.0",
        author = "https://github.com/Mofurka",
        dependsOn = {"command-handler", "player-manager"},
        description = ""
)
@Component
@EnableConfigurationProperties({PlanetProtectConfig.class})
public final class PlanetProtectPlugin implements ProxyPlugin {
}
