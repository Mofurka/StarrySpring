package irden.space.proxy.plugin.general;

import irden.space.proxy.plugin.command_handler.ChatCommand;
import irden.space.proxy.plugin.command_handler.CommandContext;
import irden.space.proxy.plugin.command_handler.CommandSpec;
import irden.space.proxy.plugin.command_handler.StringArgumentType;
import irden.space.proxy.plugin.player_manager.api.PlayerManagerApi;
import irden.space.proxy.plugin.player_manager.command.PlayerOnlineTargetArgumentType;
import irden.space.proxy.plugin.player_manager.command.PlayerTarget;
import irden.space.proxy.plugin.player_manager.model.Player;
import irden.space.proxy.protocol.payload.common.chat_header.ChatHeader;
import irden.space.proxy.protocol.payload.packet.chat.ChatReceive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static irden.space.proxy.protocol.payload.packet.chat.consts.ChatReceiveMode.WHISPER;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatCommands {
    private final PlayerManagerApi playerManagerApi;

    @ChatCommand(value = "whisper",
            aliases = "w",
            description = "Whisper to someone."
    )
    @SuppressWarnings("unused")
    public CommandSpec whisper() {
        return CommandSpec.literal("whisper")
                .then(CommandSpec.argument("player", PlayerOnlineTargetArgumentType.playerTarget(playerManagerApi))
                        .then(CommandSpec.argument("message", StringArgumentType.greedyString())
                                .executes(
                                        this::handleWhisper
                                )
                        )
                ).build();
    }

    private void handleWhisper(CommandContext ctx) {
        Player sender = ctx.sender(Player.class).orElse(null);
        if (sender == null) {
            ctx.reply("Something went wrong");
            return;
        }
        Player player = ctx.get("player", PlayerTarget.class).player();
        String message = ctx.get("message", String.class);
        ChatReceive toPlayer = ChatReceive.builder()
                .header(ChatHeader.builder().mode(WHISPER).channel("").clientId(sender.clientId()).build())
                .name(sender.nickname())
                .message(message)
                .data(null)
                .build();
        ChatReceive toSender = ChatReceive.builder()
                .header(ChatHeader.builder().mode(WHISPER).channel("").clientId(sender.clientId()).build())
                .name("-> ".concat(player.nickname()))
                .message(message)
                .data(null)
                .build();
        sender.sendMessage(toSender);
        player.sendMessage(toPlayer);
        log.info("[WHISPER][{} -> {}]: {}", sender.nickname(), player.nickname(), message);
    }
}
