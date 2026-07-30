package irden.space.proxy.plugin.discord;

import irden.space.proxy.plugin.general.events.ChatMessageEvent;
import irden.space.proxy.plugin.player_manager.events.PlayerConnectedEvent;
import irden.space.proxy.plugin.player_manager.events.PlayerDisconnectedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatMessageEventHandler {
    private final DiscordBotMessageService discordBotMessageService;

    @EventListener
    @Async
    public void onPlayerConnectedEvent(PlayerConnectedEvent event) {
        String name = event.player().name();
        var joinMessage = ":inbox_tray:**%s** заходит на сервер.".formatted(name);
        discordBotMessageService.handleInGameMessage(joinMessage);
    }

    @EventListener
    @Async
    public void onChatMessageEvent(ChatMessageEvent event) {
        var finalMessage = "**<%s>[%s]:**  `%s`".formatted(event.sender().name(), event.mode(), event.message());
        discordBotMessageService.handleInGameMessage(finalMessage);
    }

    @EventListener
    @Async
    public void onPlayerDisconnectedEvent(PlayerDisconnectedEvent event) {
        String name = event.player().name();
        var exitMessage = ":outbox_tray:**%s** выходит с сервера.".formatted(name);
        discordBotMessageService.handleInGameMessage(exitMessage);
    }

}
