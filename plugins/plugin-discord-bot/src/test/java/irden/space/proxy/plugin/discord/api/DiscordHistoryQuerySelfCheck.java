package irden.space.proxy.plugin.discord.api;

public final class DiscordHistoryQuerySelfCheck {

    private DiscordHistoryQuerySelfCheck() {
    }

    @SuppressWarnings("unused")
    static void main(String[] args) {
        keepsAnchorWhenLimitOrOrderChanges();
        readsForwardFromAnchorWhenAskedForNewerMessages();
        rejectsAnchorWithoutMessageId();
        rejectsTooWideWindowAroundMessage();
        rejectsLimitOutsideAllowedRange();
    }

    static void keepsAnchorWhenLimitOrOrderChanges() {
        DiscordHistoryQuery query = DiscordHistoryQuery.before(777L, 10)
                .withLimit(25)
                .withOldestFirst();

        check(query.anchor() == DiscordHistoryQuery.Anchor.BEFORE, "anchor is kept");
        check(query.anchorMessageId() == 777L, "anchor message is kept");
        check(query.limit() == 25, "limit is replaced");
        check(query.oldestFirst(), "order is replaced");
    }

    static void readsForwardFromAnchorWhenAskedForNewerMessages() {
        DiscordHistoryQuery query = DiscordHistoryQuery.after(42L, 100);

        check(query.anchor().needsMessageId(), "after() is anchored to a message");
        check(query.oldestFirst(), "catching up on a channel is chronological by default");
    }

    static void rejectsAnchorWithoutMessageId() {
        expectRejected(() -> DiscordHistoryQuery.before(0L, 10), "before() without a message id");
        expectRejected(() -> DiscordHistoryQuery.around(-1L, 10), "around() without a message id");
    }

    static void rejectsTooWideWindowAroundMessage() {
        expectRejected(
                () -> DiscordHistoryQuery.around(1L, DiscordHistoryQuery.MAX_PAGE_SIZE + 1),
                "around() beyond a single Discord page"
        );
    }

    static void rejectsLimitOutsideAllowedRange() {
        expectRejected(() -> DiscordHistoryQuery.latest(0), "zero limit");
        expectRejected(() -> DiscordHistoryQuery.latest(DiscordHistoryQuery.MAX_LIMIT + 1), "oversized limit");
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
