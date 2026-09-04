package irden.space.proxy.plugin.discord.api;

import lombok.Builder;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

@Builder(toBuilder = true)
public record DiscordWebhook(
        long id,
        String name,
        String token,
        String avatarUrl,
        long channelId,
        long guildId,
        DiscordWebhookType type,
        long ownerId,
        String ownerName,
        boolean partial
) {

    public DiscordWebhook {
        name = name == null ? "" : name;
        type = type == null ? DiscordWebhookType.UNKNOWN : type;
    }

    public boolean executable() {
        return token != null && !token.isBlank();
    }

    public Optional<DiscordWebhookTarget> target() {
        return executable() ? Optional.of(new DiscordWebhookTarget(id, token)) : Optional.empty();
    }

    public DiscordWebhookTarget requireTarget() {
        return target().orElseThrow(() -> new DiscordUnavailableException(
                "Discord webhook " + Long.toUnsignedString(id) + " has no token and cannot be executed"));
    }

    public Optional<String> url() {
        return target().map(DiscordWebhookTarget::url);
    }

    public boolean named(String other) {
        return name.equalsIgnoreCase(other);
    }

    @Override
    public @NonNull String toString() {
        return "DiscordWebhook[id=" + Long.toUnsignedString(id)
                + ", name=" + name
                + ", token=" + (executable() ? "***" : "none")
                + ", channelId=" + Long.toUnsignedString(channelId)
                + ", guildId=" + Long.toUnsignedString(guildId)
                + ", type=" + type
                + ", ownerId=" + Long.toUnsignedString(ownerId)
                + ", ownerName=" + ownerName
                + ", partial=" + partial
                + ']';
    }
}
