package irden.space.proxy.plugin.general;

import irden.space.proxy.plugin.command_handler.ChatCommand;
import irden.space.proxy.plugin.command_handler.CommandSpec;
import irden.space.proxy.plugin.command_handler.CommandSurface;
import irden.space.proxy.plugin.command_handler.StringArgumentType;
import irden.space.proxy.plugin.general.chat.WhisperCommandHandler;
import irden.space.proxy.plugin.player_manager.api.PlayerManagerApi;
import irden.space.proxy.plugin.player_manager.command.PlayerOnlineTargetArgumentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatCommands {
    private final PlayerManagerApi playerManagerApi;
    private final WhisperCommandHandler whisperCommandHandler;


    @ChatCommand(value = "whisper",
            aliases = "w",
            description = "Whisper to someone."
    )
    @SuppressWarnings("unused")
    public CommandSpec whisper() {
        return CommandSpec.literal("whisper").surfaces(CommandSurface.IN_GAME)
                .then(CommandSpec.argument("recipient", PlayerOnlineTargetArgumentType.playerTarget(playerManagerApi))
                        .then(CommandSpec.argument("message", StringArgumentType.greedyString())
                                .executes(
                                        whisperCommandHandler::handleWhisper
                                )
                        )
                ).build();
    }

    @ChatCommand(value = "reply", aliases = "r", description = "Reply to a person that whispters you")
    public CommandSpec reply() {
        return CommandSpec.literal("reply").surfaces(CommandSurface.IN_GAME)
                .then(CommandSpec.argument("message", StringArgumentType.greedyString()).executes(
                        whisperCommandHandler::handleReply
                )).build();
    }


}
