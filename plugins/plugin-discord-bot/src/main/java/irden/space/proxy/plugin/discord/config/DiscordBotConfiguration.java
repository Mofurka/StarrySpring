package irden.space.proxy.plugin.discord.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("discord-bot")
public record DiscordBotConfiguration(
        String botToken,
        Long gameChatChannelId

) {

}
