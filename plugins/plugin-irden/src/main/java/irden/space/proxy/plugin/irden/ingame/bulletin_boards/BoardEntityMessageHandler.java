package irden.space.proxy.plugin.irden.ingame.bulletin_boards;

import irden.space.proxy.plugin.command_handler.entity_message.EntityMessageContext;
import irden.space.proxy.plugin.command_handler.entity_message.EntityMessageHandler;
import irden.space.proxy.plugin.discord.api.*;
import irden.space.proxy.plugin.irden.ingame.bulletin_boards.model.request.ForumMessagesContent;
import irden.space.proxy.plugin.irden.ingame.bulletin_boards.model.request.TextContent;
import irden.space.proxy.plugin.irden.ingame.bulletin_boards.model.response.StarboundForumMessage;
import irden.space.proxy.plugin.irden.ingame.bulletin_boards.model.response.StarboundForumPost;
import irden.space.proxy.plugin.irden.test.DiscordApplicationTokenService;
import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.codec.variant.Variants;
import irden.space.proxy.protocol.util.VariantObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static irden.space.proxy.plugin.irden.test.DiscordApplicationTokenService.extractToken;
import static irden.space.proxy.plugin.irden.test.DiscordApplicationTokenService.wrapToken;

@Component
@Slf4j
@RequiredArgsConstructor
public class BoardEntityMessageHandler {
    private static final String PREFIX = "irden:bulletin_board:";
    private static final long TEST_FORUM_CHANNEL = 1544921962948731001L;
    private static final long TEST_FORUM_THREAD = 1544922291769573457L;
    private final DiscordGateway discordGateway;
    private final VariantObjectMapper variantObjectMapper;
    private final DiscordApplicationTokenService tokenService;

    @EntityMessageHandler(PREFIX + "getNotes")
    public VariantValue getNotes(EntityMessageContext context) {
        var response = discordGateway.forumPosts(TEST_FORUM_CHANNEL)
                .thenApply(posts ->
                        posts.stream().map(
                                post -> StarboundForumPost.builder()
                                        .id(post.id())
                                        .name(post.name())
                                        .archived(post.archived())
                                        .locked(post.locked())
                                        .createdAt(post.createdAt().toEpochMilli())
                                        .messageCount(post.messageCount())
                                        .build()
                        ).toList()
                ).join();

        return variantObjectMapper.toVariant(response);
    }


    @EntityMessageHandler(PREFIX + "getNoteContent")
    public VariantValue getNoteContent(EntityMessageContext context) {
        var forumMessagesContent = variantObjectMapper.fromVariant(context.arg(0), ForumMessagesContent.class);
        var threadId = forumMessagesContent.threadId();
        var response = discordGateway.history(threadId)
                .thenApply(
                        messages ->
                                messages.stream().map(
                                        message -> {
                                            extractToken(message.content()).ifPresent(token -> {
                                                log.info("Token found in message: {}", token);
                                                long decrypt = tokenService.decrypt(token, message.messageId());
                                                log.info("Decrypted application ID: {}", decrypt);
                                            });
                                            return StarboundForumMessage.builder()
                                                    .id(message.messageId())
                                                    .author(message.author().name())
                                                    .content(message.content())
                                                    .createdAt(message.createdAt().toEpochMilli() / 1000)
                                                    .build();
                                        }
                                ).toList().reversed()
                ).join();
        return variantObjectMapper.toVariant(response);
    }

    @EntityMessageHandler(PREFIX + "sendText")
    public VariantValue sendText(EntityMessageContext context) {
        var textContent = variantObjectMapper.fromVariant(context.arg(0), TextContent.class);
        var threadId = textContent.threadId();
        var content = textContent.content();

            var response = discordGateway.webhookOrCreate(TEST_FORUM_CHANNEL, "game-relay")
                    .thenApply(DiscordWebhook::requireTarget)
                    .thenCompose(relay -> publishApplication(relay, content, threadId))
                    .thenApply(message -> {
                        log.info("Successfully published application through webhook");
                        return "Сообщение отправлено";
                    })
                    .exceptionally(throwable -> {
                        log.error("Failed to publish application through webhook", throwable);
                        throw new RuntimeException("Failed to publish application through webhook", throwable);
                    }).join();

        return Variants.of(response);
    }
    private CompletableFuture<DiscordMessageRef> publishApplication(DiscordWebhookTarget relay, String content, Long channel) {
        if (extractToken(content).isPresent()) {
            throw new IllegalStateException("Token already exists in the message");
        }

        var message = DiscordMessage.text(content);

        DiscordWebhookMessage request = DiscordWebhookMessage
                .as("Mofuro" + UUID.randomUUID(), message)
                .withAvatarUrl("https://mc-heads.net/avatar/Mofuro")
                .inThread(channel);

        return discordGateway.sendWebhook(relay, request).thenCompose(sent -> {
            long applicationId = 360;
            String token = tokenService.encrypt(applicationId, sent.messageId());

            return discordGateway.editWebhookMessage(
                    relay,
                    sent,
                    DiscordMessage.text(message.content() + "\n" + wrapToken(token))
            );
        });
    }
}
