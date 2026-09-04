package irden.space.proxy.plugin.discord.api;

import lombok.Builder;
import lombok.Singular;

import java.time.Instant;
import java.util.List;
import java.util.Optional;


@Builder(toBuilder = true)
public record DiscordForumPost(
        long id,
        long forumChannelId,
        long guildId,
        String name,
        long ownerId,
        int messageCount,
        int totalMessageCount,
        int memberCount,
        boolean archived,
        boolean locked,
        boolean pinned,
        Instant createdAt,
        Instant archivedAt,
        int autoArchiveMinutes,
        int slowmodeSeconds,
        String jumpUrl,
        @Singular List<DiscordForumTag> tags
) {

    public DiscordForumPost {
        name = name == null ? "" : name;
        tags = tags == null ? List.of() : List.copyOf(tags);
    }


    public long channelId() {
        return id;
    }


    public DiscordMessageRef starterRef() {
        return new DiscordMessageRef(id, id);
    }

    public String mention() {
        return "<#" + id + ">";
    }

    public boolean hasTag(long tagId) {
        return tags.stream().anyMatch(tag -> tag.id() == tagId);
    }

    public boolean hasTag(String tagName) {
        return tags.stream().anyMatch(tag -> tag.named(tagName));
    }

    public List<String> tagNames() {
        return tags.stream().map(DiscordForumTag::name).toList();
    }


    public Optional<Instant> archiveTime() {
        return Optional.ofNullable(archivedAt);
    }


    public boolean open() {
        return !archived && !locked;
    }
}
