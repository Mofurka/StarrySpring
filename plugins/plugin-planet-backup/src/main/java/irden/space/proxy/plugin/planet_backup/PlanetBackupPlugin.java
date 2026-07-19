package irden.space.proxy.plugin.planet_backup;

import irden.space.proxy.plugin.api.PluginDefinition;
import irden.space.proxy.plugin.api.ProxyPlugin;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@PluginDefinition(
        id = "planet-backup",
        name = "Planet Backup",
        version = "1.0.0",
        author = "https://github.com/Mofurka",
        dependsOn = {"native-server-lifespan", "command-handler", "general"}
)
@Component
@EnableConfigurationProperties({PlanetBackupConfig.class})
public final class PlanetBackupPlugin implements ProxyPlugin {

}
