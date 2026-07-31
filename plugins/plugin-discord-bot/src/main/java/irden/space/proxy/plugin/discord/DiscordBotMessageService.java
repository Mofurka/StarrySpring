package irden.space.proxy.plugin.discord;

import irden.space.proxy.plugin.discord.config.DiscordBotConfiguration;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DiscordBotMessageService {
    private final DiscordBotRunner discordBotRunner;
    private final DiscordBotConfiguration discordBotConfiguration;

    @Async
    public void handleInGameMessage(String message) {
        Optional.ofNullable(discordBotRunner.getBot()).ifPresent(
                bot -> bot.sendMessageIntoChannelById(discordBotConfiguration.gameChatChannelId(), message));

    }


}
