package irden.space.proxy.plugin.discord.api;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;


public record DiscordForumPostQuery(
        Scope scope,
        int limit,
        Set<Long> tagIds,
        boolean oldestFirst
) {


    public static final int MAX_PAGE_SIZE = 100;
    public static final int MAX_LIMIT = 1000;

    public DiscordForumPostQuery {
        if (scope == null) {
            throw new IllegalArgumentException("Forum post scope must not be null");
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("Forum post limit must be within 1.." + MAX_LIMIT + ": " + limit);
        }
        tagIds = tagIds == null ? Set.of() : Set.copyOf(tagIds);
    }


    public static DiscordForumPostQuery active() {
        return active(MAX_LIMIT);
    }

    public static DiscordForumPostQuery active(int limit) {
        return new DiscordForumPostQuery(Scope.ACTIVE, limit, Set.of(), false);
    }


    public static DiscordForumPostQuery archived(int limit) {
        return new DiscordForumPostQuery(Scope.ARCHIVED, limit, Set.of(), false);
    }


    public static DiscordForumPostQuery all(int limit) {
        return new DiscordForumPostQuery(Scope.ALL, limit, Set.of(), false);
    }

    public DiscordForumPostQuery withLimit(int limit) {
        return new DiscordForumPostQuery(scope, limit, tagIds, oldestFirst);
    }

    public DiscordForumPostQuery withScope(Scope scope) {
        return new DiscordForumPostQuery(scope, limit, tagIds, oldestFirst);
    }


    public DiscordForumPostQuery withTags(long... tagIds) {
        return withTags(Arrays.stream(tagIds).boxed().toList());
    }

    public DiscordForumPostQuery withTags(Collection<Long> tagIds) {
        return new DiscordForumPostQuery(scope, limit, new LinkedHashSet<>(tagIds), oldestFirst);
    }


    public DiscordForumPostQuery withOldestFirst() {
        return withOldestFirst(true);
    }

    public DiscordForumPostQuery withOldestFirst(boolean oldestFirst) {
        return new DiscordForumPostQuery(scope, limit, tagIds, oldestFirst);
    }

    public boolean filtersByTag() {
        return !tagIds.isEmpty();
    }


    public boolean matches(DiscordForumPost post) {
        if (!filtersByTag()) {
            return true;
        }
        return post.tags().stream().anyMatch(tag -> tagIds.contains(tag.id()));
    }

    public enum Scope {
        ACTIVE,
        ARCHIVED,
        ALL;

        public boolean includesActive() {
            return this != ARCHIVED;
        }

        public boolean includesArchived() {
            return this != ACTIVE;
        }
    }
}
