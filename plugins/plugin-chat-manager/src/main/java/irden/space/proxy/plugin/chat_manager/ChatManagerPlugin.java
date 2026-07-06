package irden.space.proxy.plugin.chat_manager;

import irden.space.proxy.plugin.api.PluginDefinition;
import irden.space.proxy.plugin.api.ProxyPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@PluginDefinition(
        id = "chat-manager",
        name = "Chat Manager",
        version = "1.0.0",
        dependsOn = {"command-handler"},
        author = "https://github.com/Mofurka",
        description = "A plugin for managing chat functionality."
)
@Component
public final class ChatManagerPlugin implements ProxyPlugin {
    private static final Logger log = LoggerFactory.getLogger(ChatManagerPlugin.class);

}
