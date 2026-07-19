package irden.space.proxy.plugin.launcher;

import irden.space.proxy.plugin.api.PluginDefinition;
import irden.space.proxy.plugin.api.ProxyPlugin;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;


@PluginDefinition(
        id = "launcher",
        name = "Launcher",
        version = "1.0.0",
        author = "https://github.com/Mofurka",
        description = "Launcher integration. Bearer-token REST authentication (stub validation)."
)
@Component
@EnableConfigurationProperties(LauncherConfig.class)
public final class LauncherPlugin implements ProxyPlugin {

}
