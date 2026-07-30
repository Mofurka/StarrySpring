package irden.space.proxy.plugin.irden.statistic;

import irden.space.proxy.plugin.general.events.ChatMessageEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlayerStatisticEventHandler {
    private final PlayerStatisticService playerStatisticService;


    @EventListener
    public void onChatMessage(ChatMessageEvent event) {
        playerStatisticService.recordMessage(event.sender().uuid().toString(), event.message());
    }

}
