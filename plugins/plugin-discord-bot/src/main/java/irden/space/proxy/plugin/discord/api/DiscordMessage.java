package irden.space.proxy.plugin.discord.api;

import lombok.Builder;
import lombok.Singular;

import java.util.List;

@Builder(toBuilder = true)
public record DiscordMessage(
        String content,
        @Singular List<DiscordEmbed> embeds,
        @Singular List<DiscordButton> buttons
) {

    public DiscordMessage {
        embeds = embeds == null ? List.of() : List.copyOf(embeds);
        buttons = buttons == null ? List.of() : List.copyOf(buttons);
    }

    public static DiscordMessage text(String content) {
        return new DiscordMessage(content, List.of(), List.of());
    }

    public static DiscordMessage of(DiscordEmbed... embeds) {
        return new DiscordMessage(null, List.of(embeds), List.of());
    }

    public boolean isEmpty() {
        return (content == null || content.isBlank()) && embeds.isEmpty();
    }
}
