package irden.space.proxy.plugin.ban_manager;

import irden.space.proxy.plugin.api.PluginDefinition;
import irden.space.proxy.plugin.api.ProxyPlugin;

/**
 * Ban-manager entry point.
 */
@PluginDefinition(
        id = "ban-manager",
        name = "Ban Manager",
        version = "1.0.0",
        description = "An advanced ban system that allows banning players by name, UUID, or IP address, with support for temporary and permanent bans.",
        author = "https://github.com/Mofurka",
        dependsOn = {"player-manager"}
)
public final class BanManagerPlugin implements ProxyPlugin {
}
