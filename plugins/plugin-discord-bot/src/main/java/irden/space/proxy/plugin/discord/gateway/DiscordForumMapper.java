package irden.space.proxy.plugin.discord.gateway;

import irden.space.proxy.plugin.discord.api.*;
import net.dv8tion.jda.api.entities.channel.attribute.IPostContainer;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.forums.ForumTag;
import net.dv8tion.jda.api.entities.channel.forums.ForumTagSnowflake;
import net.dv8tion.jda.api.entities.emoji.EmojiUnion;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

final class DiscordForumMapper {

    private DiscordForumMapper() {
    }

    static DiscordForumChannel toForumChannel(ForumChannel forum) {
        Category category = forum.getParentCategory();
        EmojiUnion defaultReaction = forum.getDefaultReaction();

        return DiscordForumChannel.builder()
                .id(forum.getIdLong())
                .guildId(forum.getGuild().getIdLong())
                .guildName(forum.getGuild().getName())
                .name(forum.getName())
                .topic(forum.getTopic())
                .categoryId(forum.getParentCategoryIdLong())
                .categoryName(category == null ? null : category.getName())
                .position(forum.getPositionRaw())
                .nsfw(forum.isNSFW())
                .tagRequired(forum.isTagRequired())
                .slowmodeSeconds(forum.getSlowmode())
                .defaultPostSlowmodeSeconds(forum.getDefaultThreadSlowmode())
                .defaultReactionEmoji(defaultReaction == null ? null : defaultReaction.getFormatted())
                .defaultSortOrder(toSortOrder(forum.getDefaultSortOrder()))
                .defaultLayout(toLayout(forum.getDefaultLayout()))
                .availableTags(toTags(forum.getAvailableTags()))
                .build();
    }

    static List<DiscordForumChannel> toForumChannels(Collection<ForumChannel> forums) {
        List<DiscordForumChannel> result = new ArrayList<>(forums.size());
        for (ForumChannel forum : forums) {
            result.add(toForumChannel(forum));
        }
        return result;
    }

    static DiscordForumPost toPost(ThreadChannel thread) {
        return DiscordForumPost.builder()
                .id(thread.getIdLong())
                .forumChannelId(thread.getParentChannel().getIdLong())
                .guildId(thread.getGuild().getIdLong())
                .name(thread.getName())
                .ownerId(thread.getOwnerIdLong())
                .messageCount(thread.getMessageCount())
                .totalMessageCount(thread.getTotalMessageCount())
                .memberCount(thread.getMemberCount())
                .archived(thread.isArchived())
                .locked(thread.isLocked())
                .pinned(thread.isPinned())
                .createdAt(toInstant(thread.getTimeCreated()))
                .archivedAt(thread.isArchived() ? toInstant(thread.getTimeArchiveInfoLastModified()) : null)
                .autoArchiveMinutes(thread.getAutoArchiveDuration().getMinutes())
                .slowmodeSeconds(thread.getSlowmode())
                .jumpUrl(thread.getJumpUrl())
                .tags(toTags(thread.getAppliedTags()))
                .build();
    }

    static List<DiscordForumPost> toPosts(Collection<ThreadChannel> threads) {
        List<DiscordForumPost> result = new ArrayList<>(threads.size());
        for (ThreadChannel thread : threads) {
            result.add(toPost(thread));
        }
        return result;
    }

    static List<ForumTagSnowflake> toTagSnowflakes(Collection<Long> tagIds) {
        List<ForumTagSnowflake> result = new ArrayList<>(tagIds.size());
        for (Long tagId : tagIds) {
            result.add(ForumTagSnowflake.fromId(tagId));
        }
        return result;
    }

    private static List<DiscordForumTag> toTags(Collection<ForumTag> tags) {
        List<DiscordForumTag> result = new ArrayList<>(tags.size());
        for (ForumTag tag : tags) {
            EmojiUnion emoji = tag.getEmoji();
            result.add(new DiscordForumTag(
                    tag.getIdLong(),
                    tag.getName(),
                    emoji == null ? null : emoji.getFormatted(),
                    tag.isModerated(),
                    tag.getPosition()
            ));
        }
        return result;
    }

    private static DiscordForumSortOrder toSortOrder(IPostContainer.SortOrder sortOrder) {
        return switch (sortOrder) {
            case RECENT_ACTIVITY -> DiscordForumSortOrder.RECENT_ACTIVITY;
            case CREATION_TIME -> DiscordForumSortOrder.CREATION_TIME;
            case UNKNOWN -> DiscordForumSortOrder.UNKNOWN;
        };
    }

    private static DiscordForumLayout toLayout(ForumChannel.Layout layout) {
        return switch (layout) {
            case DEFAULT_VIEW -> DiscordForumLayout.DEFAULT_VIEW;
            case LIST_VIEW -> DiscordForumLayout.LIST_VIEW;
            case GALLERY_VIEW -> DiscordForumLayout.GALLERY_VIEW;
            case UNKNOWN -> DiscordForumLayout.UNKNOWN;
        };
    }

    private static Instant toInstant(OffsetDateTime time) {
        return time == null ? null : time.toInstant();
    }
}
