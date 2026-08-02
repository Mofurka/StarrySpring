package irden.space.proxy.plugin.discord.api;

import java.util.Objects;

public record DiscordEmbedField(String name, String value, boolean inline) {

    public DiscordEmbedField {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(value, "value");
    }

    public static DiscordEmbedField of(String name, String value) {
        return new DiscordEmbedField(name, value, false);
    }

    public static DiscordEmbedField inline(String name, String value) {
        return new DiscordEmbedField(name, value, true);
    }
}
