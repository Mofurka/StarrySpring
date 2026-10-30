package irden.space.proxy.plugin.irden.ingame.bulletin_boards;

import irden.space.proxy.plugin.command_handler.entity_message.EntityMessageContext;
import irden.space.proxy.plugin.command_handler.entity_message.EntityMessageHandler;
import irden.space.proxy.plugin.discord.api.*;
import irden.space.proxy.plugin.irden.constants.PlayerMetadata;
import irden.space.proxy.plugin.irden.ingame.bulletin_boards.model.request.ForumChannelData;
import irden.space.proxy.plugin.irden.ingame.bulletin_boards.model.request.ForumMessagesContent;
import irden.space.proxy.plugin.irden.ingame.bulletin_boards.model.request.NoteContent;
import irden.space.proxy.plugin.irden.ingame.bulletin_boards.model.request.TextContent;
import irden.space.proxy.plugin.irden.ingame.bulletin_boards.model.response.StarboundForumMessage;
import irden.space.proxy.plugin.irden.ingame.bulletin_boards.model.response.StarboundForumPost;
import irden.space.proxy.plugin.irden.test.DiscordApplicationTokenService;
import irden.space.proxy.plugin.player_manager.api.PlayerManagerApi;
import irden.space.proxy.plugin.player_manager.model.Player;
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
    private final PlayerManagerApi playerManagerApi;

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

    @EntityMessageHandler(PREFIX + "createNote")
    public VariantValue createNote(EntityMessageContext context) {
        var noteContent = variantObjectMapper.fromVariant(context.arg(0), NoteContent.class);
        var appId = requireAppId(context);

        var response = discordGateway.webhookOrCreate(TEST_FORUM_CHANNEL, "game-relay")
                .thenApply(DiscordWebhook::requireTarget)
                .thenCompose(relay -> createApplication(relay, noteContent, appId))
                .thenApply(post -> {
                    log.info("Successfully created forum post {} through webhook", post.channelId());
                    return "Объявление создано!";
                })
                .exceptionally(throwable -> {
                    log.error("Failed to create forum post through webhook", throwable);
                    throw new RuntimeException("Failed to create forum post through webhook", throwable);
                }).join();

        return variantObjectMapper.toVariant(response);
    }

    private CompletableFuture<DiscordMessageRef> createApplication(DiscordWebhookTarget relay, NoteContent noteContent, long appId) {
        if (extractToken(noteContent.content()).isPresent()) {
            throw new IllegalStateException("Token already exists in the message");
        }

        var message = DiscordMessage.text(noteContent.content());

        return discordGateway.createForumPost(
                relay,
                DiscordForumPostRequest.of(noteContent.name(), message),
                "Инкогнито.",
                "https://i.imgur.com/bNdKwD5.png"
        ).thenCompose(post -> {

            String token = tokenService.encrypt(appId, post.messageId());

            return discordGateway.editWebhookMessage(
                    relay,
                    post,
                    DiscordMessage.text(message.content() + "\n" + wrapToken(token))
            );
        });
    }

    @EntityMessageHandler(PREFIX + "sendText")
    public VariantValue sendText(EntityMessageContext context) {
        var textContent = variantObjectMapper.fromVariant(context.arg(0), TextContent.class);
        var threadId = textContent.threadId();
        var content = textContent.content();
        var appId = requireAppId(context);

        var response = discordGateway.webhookOrCreate(TEST_FORUM_CHANNEL, "game-relay")
                .thenApply(DiscordWebhook::requireTarget)
                .thenCompose(relay -> publishApplication(relay, content, threadId, appId))
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
                        editApplication(relay, content, new DiscordMessageRef(threadId, messageId), requireAppId(context)))
                .thenApply(message -> {
                    log.info("Successfully edited application through webhook");
                    return "Сообщение изменено";
                })
                .exceptionally(throwable -> {
                    log.error("Failed to edit application through webhook", throwable);
                    throw new RuntimeException("Failed to edit application through webhook", throwable);
                }).join();

        return Variants.of(response);
    }


    private CompletableFuture<DiscordMessageRef> editApplication(DiscordWebhookTarget relay, String content, DiscordMessageRef messageRef, long appId) {
        return requireOwnToken(relay, messageRef, appId).thenCompose(token -> {
            var finalMessage = content + "\n" + wrapToken(token);
            return discordGateway.editWebhookMessage(relay, messageRef, DiscordMessage.text(finalMessage));
        });
    }

    @EntityMessageHandler(PREFIX + "deleteNote")
    public VariantValue deleteNote(EntityMessageContext context) {
        var threadId = variantObjectMapper.fromVariant(context.arg(0), ForumMessagesContent.class).threadId();
        var starterRef = new DiscordMessageRef(threadId, threadId);
        var appId = requireAppId(context);

        var response = discordGateway.webhookOrCreate(TEST_FORUM_CHANNEL, "game-relay")
                .thenApply(DiscordWebhook::requireTarget)
                .thenCompose(relay -> requireOwnToken(relay, starterRef, appId))
                .thenCompose(token -> discordGateway.deleteForumPost(TEST_FORUM_CHANNEL, threadId))
                .thenApply(ignored -> {
                    log.info("Successfully deleted forum post {}", threadId);
                    return "Объявление удалено";
                })
                .exceptionally(throwable -> {
                    log.error("Failed to delete forum post {}", threadId, throwable);
                    throw new RuntimeException("Failed to delete forum post", throwable);
                }).join();

        return Variants.of(response);
    }

    @EntityMessageHandler(PREFIX + "deleteText")
    public VariantValue deleteText(EntityMessageContext context) {
        var textContent = variantObjectMapper.fromVariant(context.arg(0), TextContent.class);
        var messageRef = new DiscordMessageRef(textContent.threadId(), textContent.messageId());
        if (messageRef.messageId() == messageRef.channelId()) {
            throw new IllegalStateException("Первое сообщение нельзя удалить отдельно, удалите объявление целиком.");
        }
        var appId = requireAppId(context);

        var response = discordGateway.webhookOrCreate(TEST_FORUM_CHANNEL, "game-relay")
                .thenApply(DiscordWebhook::requireTarget)
                .thenCompose(relay -> requireOwnToken(relay, messageRef, appId)
                        .thenCompose(token -> discordGateway.deleteWebhookMessage(relay, messageRef)))
                .thenApply(ignored -> {
                    log.info("Successfully deleted application message through webhook");
                    return "Сообщение удалено";
                })
                .exceptionally(throwable -> {
                    log.error("Failed to delete application message through webhook", throwable);
                    throw new RuntimeException("Failed to delete application message through webhook", throwable);
                }).join();

        return Variants.of(response);
    }

    private CompletableFuture<String> requireOwnToken(DiscordWebhookTarget relay, DiscordMessageRef messageRef, long appId) {
        return discordGateway.fetchWebhookMessage(relay, messageRef).thenApply(messageOptional -> messageOptional
                .flatMap(message -> extractToken(message.content())
                        .filter(token -> tokenService.decrypt(token, message.messageId()) == appId))
                .orElseThrow(() -> new IllegalStateException("Неверный токен или сообщение не ваше.")));
    }


    private CompletableFuture<DiscordMessageRef> publishApplication(DiscordWebhookTarget relay, String content, Long channel, Long appId) {
        if (extractToken(content).isPresent()) {
            throw new IllegalStateException("Token already exists in the message");
        }

        var message = DiscordMessage.text(content);

        DiscordWebhookMessage request = DiscordWebhookMessage
                .as("Инкогнито.", message)
                .withAvatarUrl("https://i.imgur.com/bNdKwD5.png")
                .inThread(channel);

        return discordGateway.sendWebhook(relay, request).thenCompose(sent -> {

            String token = tokenService.encrypt(appId, sent.messageId());

            return discordGateway.editWebhookMessage(
                    relay,
                    sent,
                    DiscordMessage.text(message.content() + "\n" + wrapToken(token))
            );
        });
    }

    private Player requirePlayer(EntityMessageContext context) {
        String s = context.session().sessionId();
        return playerManagerApi.findPlayerBySessionId(s).orElseThrow(() -> new IllegalStateException("Player not found"));
    }

    private Long requireAppId(EntityMessageContext context) {
        Player player = requirePlayer(context);
        return PlayerMetadata.getApplicationId(player);
    }

}
