package irden.space.proxy.plugin.general;

import irden.space.proxy.plugin.api.PluginDefinition;
import irden.space.proxy.plugin.api.ProxyPlugin;
import irden.space.proxy.plugin.command_handler.ChatCommand;
import irden.space.proxy.plugin.command_handler.CommandSpec;
import irden.space.proxy.plugin.command_handler.StringArgumentType;
import irden.space.proxy.plugin.player_manager.api.PlayerManagerApi;
import irden.space.proxy.protocol.payload.packet.chat.ChatReceive;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@PluginDefinition(
        id = "general",
        name = "General Plugin",
        version = "1.0.0",
        dependsOn = {"command-handler", "player-manager"},
        author = "https://github.com/Mofurka",
        description = "A plugin for enhance chat functionality and it manage and other things."
)
@Component
public final class GeneralPlugin implements ProxyPlugin {

}
