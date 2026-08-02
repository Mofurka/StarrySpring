package irden.space.proxy.plugin.general;

import irden.space.proxy.plugin.api.PacketDecision;
import irden.space.proxy.plugin.api.PacketInterceptionContext;
import irden.space.proxy.plugin.api.annotations.PacketHandler;
import irden.space.proxy.plugin.general.events.ChatMessageEvent;
import irden.space.proxy.plugin.general.events.CleanChatMessageEvent;
import irden.space.proxy.plugin.general.permissions.ChatPermissions;
import irden.space.proxy.plugin.player_manager.api.PlayerManagerApi;
import irden.space.proxy.plugin.player_manager.model.Player;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketType;
import irden.space.proxy.protocol.payload.common.chat_header.ChatHeader;
import irden.space.proxy.protocol.payload.packet.chat.ChatReceive;
import irden.space.proxy.protocol.payload.packet.chat.ChatSent;
import irden.space.proxy.protocol.payload.packet.chat.consts.ChatReceiveMode;
import irden.space.proxy.protocol.payload.packet.chat.consts.ChatSentMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;

import static irden.space.proxy.plugin.command_handler.CommandHandlerPlugin.COMMAND_PREFIX;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatInterceptor {
    private final MessageSource messageSource;
    private final PlayerManagerApi playerManager;
    private final ApplicationEventPublisher publisher;
    private final GeneralUtils generalUtils;

    @PacketHandler(
            value = PacketType.CHAT_SENT,
            direction = PacketDirection.TO_SERVER
    )
    @SuppressWarnings("unused")
    public PacketDecision onChatSent(PacketInterceptionContext ctx) {
        ChatSent chatSent = ctx.parsedPayload(ChatSent.class);
        if (chatSent != null && !chatSent.content().startsWith(COMMAND_PREFIX)) {
            Optional<Player> playerBySessionId = playerManager.findPlayerBySessionId(ctx.session().sessionId());
            if (playerBySessionId.isPresent()) {
                var player = playerBySessionId.get();
                if (chatSent.mode().equals(ChatSentMode.UNIVERSE) && !ctx.session().permissions().has(ChatPermissions.UNIVERSE_CHAT.permission())) {

                    String message = messageSource.getMessage("chat.universe_blocked", null, Locale.getDefault());

                    return PacketDecision.cancel();
                }
                var chatMessageEvent = new ChatMessageEvent(player, chatSent.mode().name(), chatSent.content());
                publisher.publishEvent(chatMessageEvent);
            }
        }

        return PacketDecision.forward();
    }

    @EventListener
    public void onChatSentLogger(ChatMessageEvent event) {
        log.info("[{}][{}]: {}", event.mode(), event.sender().nickname(), event.message());
    }

    @EventListener
    public void onCleanChatMessageEvent(CleanChatMessageEvent event) {
        ChatReceive build = ChatReceive.builder()
                .header(ChatHeader.builder().mode(ChatReceiveMode.BROADCAST).clientId(0).build())
                .name(event.sender())
                .message(event.message())
                .build();
        generalUtils.broadcastMessage(build);
    }

}
