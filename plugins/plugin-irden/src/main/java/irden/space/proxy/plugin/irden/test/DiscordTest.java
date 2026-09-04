//package irden.space.proxy.plugin.irden.test;
//
//import irden.space.proxy.plugin.api.annotations.OnStart;
//import irden.space.proxy.plugin.discord.api.*;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//
//import java.util.concurrent.CompletableFuture;
//
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public final class DiscordTest {
//    private static final long TEST_FORUM_CHANNEL = 1544921962948731001L;
//    private static final long TEST_FORUM_THREAD = 1544922291769573457L;
//
//    private final DiscordGateway discordGateway;
//    private final DiscordApplicationTokenService tokenService;
//
//
//    @OnStart
//    public void test2() {
//        discordGateway.whenReady().thenRun(() -> {
//            log.info("Irden Plugin started");
//
////            discordGateway.forumPosts(TEST_FORUM_CHANNEL)
////                    .thenAccept(posts -> log.info("Fetched forum posts: {}", posts))
////                    .exceptionally(throwable -> {
////                        log.error("Failed to fetch forum posts", throwable);
////                        return null;
////                    });
//
//            discordGateway.webhookOrCreate(TEST_FORUM_CHANNEL, "game-relay")
//                    .thenApply(DiscordWebhook::requireTarget)
//                    .thenCompose(this::publishApplication)
//                    .exceptionally(throwable -> {
//                        log.error("Failed to publish application through webhook", throwable);
//                        return null;
//                    });
//
////            discordGateway.send(TEST_FORUM_THREAD, "TestMessage")
////                    .exceptionally(throwable -> {
////                        log.error("Failed to send message to forum post", throwable);
////                        return null;
////                    });
//        });
//    }
//
//
//    private CompletableFuture<DiscordMessageRef> publishApplication(DiscordWebhookTarget relay) {
//        var message = DiscordMessage.text("# Test message");
//
//        DiscordWebhookMessage request = DiscordWebhookMessage
//                .as("Mofuro", message)
//                .withAvatarUrl("https://mc-heads.net/avatar/Mofuro")
//                .inThread(TEST_FORUM_THREAD);
//
//        return discordGateway.sendWebhook(relay, request).thenCompose(sent -> {
//            long applicationId = 360;
//            String token = tokenService.encrypt(applicationId, sent.messageId());
//
//            return discordGateway.editWebhookMessage(
//                    relay,
//                    sent,
//                    DiscordMessage.text(message.content() + "\n||v1." + token + "||")
//            );
//        });
//    }
//}
