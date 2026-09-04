package irden.space.proxy.plugin.discord.api;

import lombok.Builder;
import lombok.Singular;

import java.util.List;
import java.util.Optional;


@Builder(toBuilder = true)
public record DiscordForumChannel(
        long id,
        long guildId,
        String guildName,
        String name,
        String topic,
        long categoryId,
        String categoryName,
        int position,
        boolean nsfw,
        boolean tagRequired,
        int slowmodeSeconds,
        int defaultPostSlowmodeSeconds,
        String defaultReactionEmoji,
        DiscordForumSortOrder defaultSortOrder,
        DiscordForumLayout defaultLayout,
        @Singular List<DiscordForumTag> availableTags
) {

    public DiscordForumChannel {
        name = name == null ? "" : name;
        availableTags = availableTags == null ? List.of() : List.copyOf(availableTags);
        defaultSortOrder = defaultSortOrder == null ? DiscordForumSortOrder.UNKNOWN : defaultSortOrder;
        defaultLayout = defaultLayout == null ? DiscordForumLayout.UNKNOWN : defaultLayout;
    }

    public String mention() {
        return "<#" + id + ">";
    }

    public boolean hasTopic() {
        return topic != null && !topic.isBlank();
    }


    public boolean inCategory() {
        return categoryId > 0;
    }

    public Optional<DiscordForumTag> tag(long tagId) {
        return availableTags.stream().filter(tag -> tag.id() == tagId).findFirst();
    }

    public Optional<DiscordForumTag> tag(String tagName) {
        return availableTags.stream().filter(tag -> tag.named(tagName)).findFirst();
    }


    public Optional<Long> tagId(String tagName) {
        return tag(tagName).map(DiscordForumTag::id);
    }
}
