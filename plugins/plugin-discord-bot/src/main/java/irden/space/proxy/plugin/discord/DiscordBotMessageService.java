package irden.space.proxy.plugin.discord;

import irden.space.proxy.plugin.discord.api.DiscordGateway;
import irden.space.proxy.plugin.discord.config.DiscordBotConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class DiscordBotMessageService {

    private final DiscordGateway discordGateway;
    private final DiscordBotConfiguration discordBotConfiguration;

    @Async
    public void handleInGameMessage(String message) {
        Long channelId = discordBotConfiguration.gameChatChannelId();
        if (channelId == null || !discordGateway.isReady()) {
            return;
        }

        discordGateway.send(channelId, message)
                .exceptionally(failure -> {
                    log.warn("Failed to relay in-game message to Discord: {}", failure.getMessage());
                    return null;
                });
    }
}
