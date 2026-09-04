package irden.space.proxy.plugin.discord.api;

import lombok.Builder;
import lombok.Singular;

import java.util.Arrays;
import java.util.List;

@Builder(toBuilder = true)
public record DiscordForumPostRequest(
        String name,
        DiscordMessage message,
        @Singular List<Long> tagIds
) {


    public static final int MAX_NAME_LENGTH = 100;
    public static final int MAX_TAGS = 5;

    public DiscordForumPostRequest {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Forum post needs a name");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "Forum post name is limited to " + MAX_NAME_LENGTH + " characters: " + name.length());
        }
        if (message == null || message.isEmpty()) {
            throw new IllegalArgumentException("Forum post needs a starter message");
        }
        tagIds = tagIds == null ? List.of() : List.copyOf(tagIds);
        if (tagIds.size() > MAX_TAGS) {
            throw new IllegalArgumentException("Forum post is limited to " + MAX_TAGS + " tags: " + tagIds.size());
        }
    }

    public static DiscordForumPostRequest of(String name, String content) {
        return of(name, DiscordMessage.text(content));
    }

    public static DiscordForumPostRequest of(String name, DiscordEmbed embed) {
        return of(name, DiscordMessage.of(embed));
    }

    public static DiscordForumPostRequest of(String name, DiscordMessage message) {
        return new DiscordForumPostRequest(name, message, List.of());
    }

    public DiscordForumPostRequest withTags(long... tagIds) {
        return new DiscordForumPostRequest(name, message, Arrays.stream(tagIds).boxed().toList());
    }
}
