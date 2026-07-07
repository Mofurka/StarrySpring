package irden.space.proxy.plugin.chat_manager;

import irden.space.proxy.plugin.api.PluginDefinition;
import irden.space.proxy.plugin.api.ProxyPlugin;
import irden.space.proxy.plugin.player_manager.api.PlayerManagerApi;
import irden.space.proxy.protocol.payload.packet.chat.ChatReceive;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@PluginDefinition(
        id = "chat-manager",
        name = "Chat Manager",
        version = "1.0.0",
        dependsOn = {"command-handler", "player-manager"},
        author = "https://github.com/Mofurka",
        description = "A plugin for managing chat functionality."
)
@Component
@RequiredArgsConstructor
public final class ChatManagerPlugin implements ProxyPlugin {
    private static final Logger log = LoggerFactory.getLogger(ChatManagerPlugin.class);
    private final PlayerManagerApi playerManagerApi;


    public void broadcastMessage(@NotBlank String message) {
        playerManagerApi.onlinePlayers().forEach(player -> player.sendMessage(message));
    }
    public void broadcastMessage(@NotBlank ChatReceive message) {
        playerManagerApi.onlinePlayers().forEach(player -> player.sendMessage(message));
    }

}
