package irden.space.proxy.plugin.chat_manager;

import irden.space.proxy.plugin.api.PacketDecision;
import irden.space.proxy.plugin.api.PacketInterceptionContext;
import irden.space.proxy.plugin.api.annotations.PacketHandler;
import irden.space.proxy.plugin.command_handler.CommandHandlerPlugin;
import irden.space.proxy.plugin.player_manager.api.PlayerManagerApi;
import irden.space.proxy.plugin.player_manager.model.Player;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketType;
import irden.space.proxy.protocol.payload.packet.chat.ChatSent;
import irden.space.proxy.protocol.payload.packet.chat.consts.ChatSentMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;

import static irden.space.proxy.plugin.command_handler.CommandHandlerPlugin.COMMAND_PREFIX;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatInterceptor {
    private final MessageSource messageSource;
    private final PlayerManagerApi playerManager;

    @PacketHandler(
            value = PacketType.CHAT_SENT,
            direction = PacketDirection.TO_SERVER
    )
    @SuppressWarnings("unused")
    public PacketDecision onChatSent(PacketInterceptionContext ctx) {
        ChatSent chatSent = ctx.parsedPayload(ChatSent.class);
        if (chatSent != null) {

            if (!chatSent.content().startsWith(COMMAND_PREFIX) &&
                    chatSent.mode().equals(ChatSentMode.UNIVERSE) &&
                    !ctx.session().permissions().has(ChatPermissions.UNIVERSE_CHAT.permission())) {

                String message = messageSource.getMessage("chat.universe_blocked", null, Locale.getDefault());
                playerManager.getPlayerBySessionId(ctx.session().sessionId()).ifPresent(player -> {
                    player.sendMessage(message);
                });
                return PacketDecision.cancel();
            }
            onChatSentLogger(ctx, chatSent);
        }
        return PacketDecision.forward();
    }

    public void onChatSentLogger(PacketInterceptionContext ctx, ChatSent sent) {
        ChatSentMode mode = sent.mode();
        playerManager.getPlayerBySessionId(ctx.session().sessionId())
                .ifPresentOrElse(
                        (p) -> log.info("[{}][{}]: {}", mode.name(), p.nickname(), sent.content()),
                        () -> log.info("Player for message not found! Actually this message should newer appear in the log")
                );

    }

}
