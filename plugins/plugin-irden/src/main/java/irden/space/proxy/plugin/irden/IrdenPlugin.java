package irden.space.proxy.plugin.irden;

import irden.space.proxy.plugin.api.PluginDefinition;
import irden.space.proxy.plugin.api.ProxyPlugin;
import org.springframework.stereotype.Component;

@PluginDefinition(
        id = "irden",
        name = "Irden Plugin",
        version = "1.0.0",
        author = "https://github.com/Mofurka",
        dependsOn = {"command-handler", "player-manager", "discord-bot", "general", "star-custom-chat", "native-server-lifespan"},
        description = "Irden gavno ebanoe"
)
@Component

public final class IrdenPlugin implements ProxyPlugin {

}
