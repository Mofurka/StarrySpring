package irden.space.proxy.plugin.star_custom_chat;

import irden.space.proxy.plugin.api.PluginDefinition;
import irden.space.proxy.plugin.api.ProxyPlugin;
import org.springframework.stereotype.Component;

@PluginDefinition(
        id = "star-custom-chat",
        name = "Star Custom Chat",
        version = "1.0.0",
        dependsOn = {"command-handler", "player-manager", "general"},
        author = "https://github.com/Mofurka",
        description = "A star custom chat interceptor plugin that can handle star custom chat messages via stagehand entity spawnd and ems."
)
@Component
public final class StarCustomChatPlugin implements ProxyPlugin {


}
