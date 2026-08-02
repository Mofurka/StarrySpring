package irden.space.proxy.plugin.discord.api;

import lombok.Builder;
import lombok.Singular;

import java.time.Instant;
import java.util.List;

@Builder(toBuilder = true)
public record DiscordEmbed(
        String title,
        String titleUrl,
        String description,
        Integer color,
        String authorName,
        String authorUrl,
        String authorIconUrl,
        String thumbnailUrl,
        String imageUrl,
        String footerText,
        String footerIconUrl,
        Instant timestamp,
        @Singular("addField") List<DiscordEmbedField> fields
) {

    public DiscordEmbed {
        fields = fields == null ? List.of() : List.copyOf(fields);
    }

    public static DiscordEmbed of(String title, String description) {
        return DiscordEmbed.builder().title(title).description(description).build();
    }

    public static class DiscordEmbedBuilder {

        public DiscordEmbedBuilder field(DiscordEmbedField field) {
            return addField(field);
        }

        public DiscordEmbedBuilder field(String name, String value, boolean inline) {
            return addField(new DiscordEmbedField(name, value, inline));
        }

        public DiscordEmbedBuilder field(String name, String value) {
            return addField(new DiscordEmbedField(name, value, false));
        }
    }
}
