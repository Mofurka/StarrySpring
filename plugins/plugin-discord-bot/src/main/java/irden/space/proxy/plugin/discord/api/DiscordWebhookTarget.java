package irden.space.proxy.plugin.discord.api;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record DiscordWebhookTarget(long id, String token) {

    private static final Pattern URL = Pattern.compile(
            "https?://(?:[^\\s.]+\\.)?discord(?:app)?\\.com/api(?:/v\\d+)?/webhooks/(?<id>\\d+)/(?<token>[^\\s/?]+)",
            Pattern.CASE_INSENSITIVE);

    public DiscordWebhookTarget {
        if (id <= 0) {
            throw new IllegalArgumentException("Webhook id must be positive: " + id);
        }
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Webhook " + id + " has no token to execute with");
        }
    }

    public static DiscordWebhookTarget of(long id, String token) {
        return new DiscordWebhookTarget(id, token);
    }

    public static DiscordWebhookTarget ofUrl(String webhookUrl) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            throw new IllegalArgumentException("Webhook url must not be blank");
        }

        Matcher matcher = URL.matcher(webhookUrl.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Not a Discord webhook url");
        }

        return new DiscordWebhookTarget(Long.parseUnsignedLong(matcher.group("id")), matcher.group("token"));
    }

    public String url() {
        return "https://discord.com/api/webhooks/" + Long.toUnsignedString(id) + "/" + token;
    }

    @Override
    public String toString() {
        return "DiscordWebhookTarget[id=" + Long.toUnsignedString(id) + ", token=***]";
    }
}
