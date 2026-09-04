package irden.space.proxy.plugin.discord.api;

public final class DiscordWebhookTargetSelfCheck {

    private static final String TOKEN = "s3cr3t-token_value";

    private DiscordWebhookTargetSelfCheck() {
    }

    @SuppressWarnings("unused")
    static void main(String[] args) {
        readsIdAndTokenBackFromTheUrlDiscordGaveUs();
        acceptsTheUrlShapesDiscordActuallyHandsOut();
        rejectsAnythingThatIsNotAWebhookUrl();
        keepsTheTokenOutOfLogs();
        buildsExecutableTargetOnlyFromWebhookWithToken();
    }

    static void readsIdAndTokenBackFromTheUrlDiscordGaveUs() {
        DiscordWebhookTarget target = DiscordWebhookTarget.of(1544921962948731001L, TOKEN);
        DiscordWebhookTarget parsed = DiscordWebhookTarget.ofUrl(target.url());

        check(parsed.equals(target), "url round-trips back into id and token");
    }

    static void acceptsTheUrlShapesDiscordActuallyHandsOut() {
        check(
                DiscordWebhookTarget.ofUrl("https://discordapp.com/api/webhooks/123/" + TOKEN).id() == 123L,
                "the legacy discordapp.com host is accepted"
        );
        check(
                DiscordWebhookTarget.ofUrl("  https://discord.com/api/v10/webhooks/123/" + TOKEN + "  ").id() == 123L,
                "a versioned url with stray spaces is accepted"
        );
    }

    static void rejectsAnythingThatIsNotAWebhookUrl() {
        expectRejected(() -> DiscordWebhookTarget.ofUrl("https://example.com/api/webhooks/123/" + TOKEN), "a foreign host");
        expectRejected(() -> DiscordWebhookTarget.ofUrl("https://discord.com/api/webhooks/123"), "a url without a token");
        expectRejected(() -> DiscordWebhookTarget.ofUrl(" "), "a blank url");
        expectRejected(() -> DiscordWebhookTarget.of(0L, TOKEN), "a missing webhook id");
        expectRejected(() -> DiscordWebhookTarget.of(123L, " "), "a blank token");
    }

    static void keepsTheTokenOutOfLogs() {
        DiscordWebhookTarget target = DiscordWebhookTarget.of(123L, TOKEN);
        DiscordWebhook webhook = DiscordWebhook.builder().id(123L).name("relay").token(TOKEN).build();

        check(!target.toString().contains(TOKEN), "target does not print its token");
        check(!webhook.toString().contains(TOKEN), "webhook does not print its token");
        check(webhook.toString().contains("relay"), "webhook still prints what is safe to read");
        check(target.url().contains(TOKEN), "the url itself still carries the token");
    }

    static void buildsExecutableTargetOnlyFromWebhookWithToken() {
        DiscordWebhook withToken = DiscordWebhook.builder().id(123L).name("relay").token(TOKEN).build();
        DiscordWebhook foreign = DiscordWebhook.builder().id(456L).name("follower").build();

        check(withToken.executable(), "a webhook with a token can be written to");
        check(withToken.target().isPresent(), "a target is derived from the token");
        check(!foreign.executable(), "a webhook without a token cannot be written to");
        check(foreign.target().isEmpty(), "no token means no target");
        check(withToken.named("RELAY"), "webhooks are matched by name regardless of case");

        try {
            foreign.requireTarget();
        } catch (DiscordUnavailableException expected) {
            return;
        }
        throw new AssertionError("Expected requireTarget() to reject a webhook without a token");
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
