package irden.space.proxy.plugin.discord;

import irden.space.proxy.plugin.discord.config.DiscordBotConfiguration;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DiscordBotMessageService {
    private final DiscordBotRunner discordBotRunner;
    private final DiscordBotConfiguration discordBotConfiguration;

    @Async
    public void handleInGameMessage(String message) {
        discordBotRunner.getBot().sendMessageIntoChannelById(discordBotConfiguration.gameChatChannelId(), message);
    }


}
