package irden.space.proxy.plugin.discord.api;

import lombok.Builder;

@Builder(toBuilder = true)
public record DiscordWebhookMessage(
        DiscordMessage message,
        String username,
        String avatarUrl,
        long threadId
) {

    public DiscordWebhookMessage {
        if (message == null) {
            throw new IllegalArgumentException("Webhook message needs content");
        }
    }

    public static DiscordWebhookMessage of(DiscordMessage message) {
        return new DiscordWebhookMessage(message, null, null, 0);
    }

    public static DiscordWebhookMessage of(String content) {
        return of(DiscordMessage.text(content));
    }

    public static DiscordWebhookMessage of(DiscordEmbed embed) {
        return of(DiscordMessage.of(embed));
    }

    public static DiscordWebhookMessage as(String username, String content) {
        return as(username, DiscordMessage.text(content));
    }

    public static DiscordWebhookMessage as(String username, DiscordMessage message) {
        return new DiscordWebhookMessage(message, username, null, 0);
    }

    public DiscordWebhookMessage withUsername(String username) {
        return new DiscordWebhookMessage(message, username, avatarUrl, threadId);
    }

    public DiscordWebhookMessage withAvatarUrl(String avatarUrl) {
        return new DiscordWebhookMessage(message, username, avatarUrl, threadId);
    }

    public DiscordWebhookMessage inThread(long threadId) {
        return new DiscordWebhookMessage(message, username, avatarUrl, threadId);
    }

    public boolean overridesAuthor() {
        return username != null && !username.isBlank();
    }

    public boolean inThread() {
        return threadId > 0;
    }
}
