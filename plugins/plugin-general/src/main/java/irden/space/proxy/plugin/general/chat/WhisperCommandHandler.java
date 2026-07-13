package irden.space.proxy.plugin.general.chat;

import irden.space.proxy.plugin.command_handler.CommandContext;
import irden.space.proxy.plugin.player_manager.api.PlayerManagerApi;
import irden.space.proxy.plugin.player_manager.command.PlayerTarget;
import irden.space.proxy.plugin.player_manager.model.Player;
import irden.space.proxy.protocol.payload.common.chat_header.ChatHeader;
import irden.space.proxy.protocol.payload.packet.chat.ChatReceive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static irden.space.proxy.protocol.payload.packet.chat.consts.ChatReceiveMode.WHISPER;

@Component
@RequiredArgsConstructor
@Slf4j
public class WhisperCommandHandler {
    private final Map<String, String> whisperMap = new ConcurrentHashMap<>();
    private final PlayerManagerApi playerManagerApi;

    public void handleWhisper(CommandContext ctx) {
        Player sender = ctx.sender(Player.class).orElse(null);
        if (sender == null) {
            ctx.reply("Something went wrong");
            return;
        }
        Player recipient = ctx.get("recipient", PlayerTarget.class).player();
        String message = ctx.get("message", String.class);
        if (message == null) {
            ctx.reply("Message is empty");
        }
        this.sendWhisperMessage(sender, recipient, message);
    }

    public void handleReply(CommandContext ctx) {
        String message = ctx.get("message", String.class);
        if (message.isEmpty()) {
            ctx.reply("Message is empty");
            return;
        }

        Player sender = ctx.sender(Player.class).orElse(null);
        if (sender == null) {
            ctx.reply("Something went wrong");
            return;
        }
        final var senderUuid = sender.uuid().toString();
        var recipientUuid = whisperMap.get(senderUuid);
        if (recipientUuid == null) {
            ctx.reply("You did not send any whisper yet");
            return;
        }
        playerManagerApi.findPlayerByUuid(recipientUuid, true)
                .ifPresentOrElse(
                        recipient -> this.sendWhisperMessage(sender, recipient, message),
                        () -> {
                            ctx.reply("Recipient not found.");
                            whisperMap.remove(senderUuid);
                        }
                );

    }


    private void sendWhisperMessage(Player sender, Player recipient, String message) {
        ChatReceive toPlayer = ChatReceive.builder()
                .header(ChatHeader.builder().mode(WHISPER).channel("").clientId(sender.clientId()).build())
                .name(sender.nickname())
                .message(message)
                .data(null)
                .build();
        ChatReceive toSender = ChatReceive.builder()
                .header(ChatHeader.builder().mode(WHISPER).channel("").clientId(sender.clientId()).build())
                .name("-> ".concat(recipient.nickname()))
                .message(message)
                .data(null)
                .build();
        sender.sendMessage(toSender);
        if (sender != recipient) {
            recipient.sendMessage(toPlayer);
            var senderUuid = sender.uuid().toString();
            var recipientUuid = recipient.uuid().toString();
            whisperMap.put(recipientUuid, senderUuid);
            whisperMap.put(senderUuid, recipientUuid);
        }
        log.info("[WHISPER][{} -> {}]: {}", sender.nickname(), recipient.nickname(), message);
    }
}
