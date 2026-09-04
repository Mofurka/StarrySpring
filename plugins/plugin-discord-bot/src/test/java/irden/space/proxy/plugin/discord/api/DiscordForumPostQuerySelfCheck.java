package irden.space.proxy.plugin.discord.api;


public final class DiscordForumPostQuerySelfCheck {

    private DiscordForumPostQuerySelfCheck() {
    }

    @SuppressWarnings("unused")
    static void main(String[] args) {
        keepsScopeWhenLimitOrOrderChanges();
        readsOnlyLiveForumWithoutArchive();
        filtersPostsByAnyOfTheRequestedTags();
        rejectsLimitOutsideAllowedRange();
    }

    static void keepsScopeWhenLimitOrOrderChanges() {
        DiscordForumPostQuery query = DiscordForumPostQuery.archived(10)
                .withLimit(25)
                .withOldestFirst();

        check(query.scope() == DiscordForumPostQuery.Scope.ARCHIVED, "scope is kept");
        check(query.limit() == 25, "limit is replaced");
        check(query.oldestFirst(), "order is replaced");
    }

    static void readsOnlyLiveForumWithoutArchive() {
        DiscordForumPostQuery active = DiscordForumPostQuery.active();

        check(active.scope().includesActive(), "active() reads live posts");
        check(!active.scope().includesArchived(), "active() does not pay for the archive");
        check(DiscordForumPostQuery.all(10).scope().includesArchived(), "all() reaches the archive");
    }

    static void filtersPostsByAnyOfTheRequestedTags() {
        DiscordForumPostQuery query = DiscordForumPostQuery.active().withTags(1L, 2L);

        check(query.filtersByTag(), "tag filter is on");
        check(query.matches(postTagged(2L)), "a post with one of the tags passes");
        check(!query.matches(postTagged(3L)), "a post with a foreign tag is dropped");
        check(!query.matches(postTagged()), "an untagged post is dropped");
        check(DiscordForumPostQuery.active().matches(postTagged()), "without a filter every post passes");
    }

    static void rejectsLimitOutsideAllowedRange() {
        expectRejected(() -> DiscordForumPostQuery.active(0), "zero limit");
        expectRejected(
                () -> DiscordForumPostQuery.all(DiscordForumPostQuery.MAX_LIMIT + 1),
                "oversized limit"
        );
    }

    private static DiscordForumPost postTagged(long... tagIds) {
        DiscordForumPost.DiscordForumPostBuilder post = DiscordForumPost.builder().id(100L).name("post");
        for (long tagId : tagIds) {
            post.tag(new DiscordForumTag(tagId, "tag-" + tagId, null, false, 0));
        }
        return post.build();
    }

    private static void expectRejected(Runnable action, String what) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Expected " + what + " to be rejected");
    }

    private static void check(boolean condition, String what) {
        if (!condition) {
            throw new AssertionError("Expected that " + what);
        }
    }
}
