package irden.space.proxy.plugin.chat_manager;

import irden.space.proxy.plugin.command_handler.ChatCommand;
import irden.space.proxy.plugin.command_handler.CommandContext;
import irden.space.proxy.plugin.command_handler.CommandSpec;
import irden.space.proxy.plugin.player_manager.api.PlayerManagerApi;
import irden.space.proxy.plugin.player_manager.model.Player;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
@Slf4j
public class GeneralCommands {
    private final PlayerManagerApi playerManagerApi;
    @ChatCommand(value = "who", description = "Shows the list of players to the beggar")
    @SuppressWarnings("unused")
    public CommandSpec whoCommand() {
        return CommandSpec.literal("who").executes(this::handleWhoCommand).build();
    }

    private void handleWhoCommand(CommandContext ctx) {
        List<Player> players = playerManagerApi.onlinePlayers();

    }
}
