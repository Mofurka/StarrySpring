package irden.space.proxy.plugin.discord.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties("discord-bot")
public record DiscordBotConfiguration(
        String botToken,
        Long gameChatChannelId,
        List<Long> channelsToListen,
        Proxy proxy

) {
    public record Proxy(
            Boolean enabled,
            String proxyUrl
    ) {
        public boolean isEnabled() {
            return Boolean.TRUE.equals(enabled) && proxyUrl != null && !proxyUrl.isBlank();
        }
    }
}
