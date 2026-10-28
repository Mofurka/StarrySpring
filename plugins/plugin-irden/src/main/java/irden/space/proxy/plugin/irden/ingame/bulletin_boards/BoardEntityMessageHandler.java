package irden.space.proxy.plugin.irden.ingame.bulletin_boards;

import irden.space.proxy.plugin.command_handler.entity_message.EntityMessageContext;
import irden.space.proxy.plugin.command_handler.entity_message.EntityMessageHandler;
import irden.space.proxy.plugin.discord.api.*;
import irden.space.proxy.plugin.irden.ingame.bulletin_boards.model.request.ForumChannelData;
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

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static irden.space.proxy.plugin.irden.test.DiscordApplicationTokenService.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class BoardEntityMessageHandler {
    private static final String PREFIX = "irden:bulletin_board:";
    private static final long TEST_FORUM_CHANNEL = 1544921962948731001L;
    private final DiscordGateway discordGateway;
    private final VariantObjectMapper variantObjectMapper;
    private final DiscordApplicationTokenService tokenService;

    @EntityMessageHandler(PREFIX + "getNotes")
    public VariantValue getNotes(EntityMessageContext context) {
        var data = variantObjectMapper.fromVariant(context.arg(0), ForumChannelData.class);
        Optional<DiscordForumChannel> discordForumChannel = discordGateway.forumChannel(TEST_FORUM_CHANNEL).join();
        if (discordForumChannel.isPresent()) {
            var query = data.archived() ? DiscordForumPostQuery.archived(100) : DiscordForumPostQuery.active();
            var response = discordGateway.forumPosts(data.forumId(), query)
                    .thenApply(posts ->
                            posts.stream().map(
                                    post -> StarboundForumPost.builder()
                                            .id(post.id())
                                            .name(post.name())
                                            .archived(post.archived())
                                            .locked(post.locked())
                                            .createdAt(post.createdAt().toEpochMilli())
                                            .messageCount(post.messageCount())
                                            .starterRef(post.starterRef())
                                            .build()
                            ).toList()
                    ).join();
            VariantValue variant = variantObjectMapper.toVariant(response);
            return Variants.listOf(discordForumChannel.get().name(), variant);
        }

        return null;
    }


    @EntityMessageHandler(PREFIX + "firstMessage")
    public VariantValue firstMessage(EntityMessageContext context) {
        var threadRef = variantObjectMapper.fromVariant(context.arg(0), DiscordMessageRef.class);
        var firstMessage = discordGateway.fetchMessage(threadRef)
                .join();
        return firstMessage.map(discordReceivedMessage -> Variants.of(discordReceivedMessage.content())).orElse(null);
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
                                            String finalMessage;
                                            Optional<String> tokenOptional = extractToken(message.content());
                                            if (tokenOptional.isPresent()) {
                                                finalMessage = clearToken(message.content());
                                            } else {
                                                finalMessage = message.content();
                                            }
                                            return StarboundForumMessage.builder()
                                                    .id(message.messageId())
                                                    .author(message.author().name())
                                                    .content(finalMessage)
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


    @EntityMessageHandler(PREFIX + "editText")
    public VariantValue editText(EntityMessageContext context) {
        var textContent = variantObjectMapper.fromVariant(context.arg(0), TextContent.class);
        var threadId = textContent.threadId();
        var content = textContent.content();
        var messageId = textContent.messageId();

        var response = discordGateway.webhookOrCreate(TEST_FORUM_CHANNEL, "game-relay")
                .thenApply(DiscordWebhook::requireTarget)
                .thenCompose(relay ->
                        discordGateway.editWebhookMessage(relay, new DiscordMessageRef(threadId, messageId), DiscordMessage.text(content)))
                .thenApply(message -> {
                    log.info("Successfully published application through webhook");
                    return "Сообщение изменено";
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
                .as("Инкогнито.", message)
                .withAvatarUrl("https://i.imgur.com/bNdKwD5.png")
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
