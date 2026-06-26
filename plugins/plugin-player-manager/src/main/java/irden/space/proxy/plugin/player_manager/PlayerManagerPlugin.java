package irden.space.proxy.plugin.player_manager;

import irden.space.proxy.plugin.api.PluginDefinition;
import irden.space.proxy.plugin.api.ProxyPlugin;

/**
 * Player-manager entry point.
 */
@PluginDefinition(
        id = "player-manager",
        name = "Player Manager",
        version = "1.0.0",
        dependsOn = {"command-handler"},
        author = "https://github.com/Mofurka",
        description = "Plugin for player managing."
)
public final class PlayerManagerPlugin implements ProxyPlugin {
}
