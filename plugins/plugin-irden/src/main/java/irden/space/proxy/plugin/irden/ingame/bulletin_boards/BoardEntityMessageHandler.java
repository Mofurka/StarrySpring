package irden.space.proxy.plugin.irden.ingame.bulletin_boards;

import irden.space.proxy.plugin.command_handler.entity_message.EntityMessageContext;
import irden.space.proxy.plugin.command_handler.entity_message.EntityMessageHandler;
import irden.space.proxy.plugin.discord.api.*;
import irden.space.proxy.plugin.irden.constants.PlayerMetadata;
import irden.space.proxy.plugin.irden.ingame.bulletin_boards.cache.BulletinBoardCache;
import irden.space.proxy.plugin.irden.ingame.bulletin_boards.model.request.ForumChannelData;
import irden.space.proxy.plugin.irden.ingame.bulletin_boards.model.request.ForumMessagesContent;
import irden.space.proxy.plugin.irden.ingame.bulletin_boards.model.request.NoteContent;
import irden.space.proxy.plugin.irden.ingame.bulletin_boards.model.request.TextContent;
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

import static irden.space.proxy.plugin.irden.test.DiscordApplicationTokenService.extractToken;
import static irden.space.proxy.plugin.irden.test.DiscordApplicationTokenService.wrapToken;

@Component
@Slf4j
@RequiredArgsConstructor
public class BoardEntityMessageHandler {
    private static final String PREFIX = "irden:bulletin_board:";
    private final DiscordGateway discordGateway;
    private final BulletinBoardCache boardCache;
    private final VariantObjectMapper variantObjectMapper;
    private final DiscordApplicationTokenService tokenService;
    private final PlayerManagerApi playerManagerApi;

    @EntityMessageHandler(PREFIX + "getNotes")
    public VariantValue getNotes(EntityMessageContext context) {
        var data = variantObjectMapper.fromVariant(context.arg(0), ForumChannelData.class);
        Optional<DiscordForumChannel> discordForumChannel = discordGateway.forumChannel(data.forumId()).join();
        if (discordForumChannel.isPresent()) {
            var response = boardCache.posts(data.forumId(), data.archived()).join();
            VariantValue variant = variantObjectMapper.toVariant(response);
            return Variants.listOf(discordForumChannel.get().name(), variant);
        }

        return null;
    }


    @EntityMessageHandler(PREFIX + "firstMessage")
    public VariantValue firstMessage(EntityMessageContext context) {
        var threadRef = variantObjectMapper.fromVariant(context.arg(0), DiscordMessageRef.class);
        var firstMessage = boardCache.content(threadRef).join();
        return firstMessage.map(Variants::of).orElse(null);
    }

    @EntityMessageHandler(PREFIX + "getNoteContent")
    public VariantValue getNoteContent(EntityMessageContext context) {
        var forumMessagesContent = variantObjectMapper.fromVariant(context.arg(0), ForumMessagesContent.class);
        var response = boardCache.messages(forumMessagesContent.threadId()).join();
        return variantObjectMapper.toVariant(response);
    }

    @EntityMessageHandler(PREFIX + "createNote")
    public VariantValue createNote(EntityMessageContext context) {
        var noteContent = variantObjectMapper.fromVariant(context.arg(0), NoteContent.class);
        var forumId = noteContent.forumId();
        var appId = requireAppId(context);

        var response = boardCache.relay(forumId)
                .thenCompose(relay -> createApplication(relay, noteContent, appId))
                .thenApply(post -> {
                    log.info("Successfully created forum post {} through webhook", post.channelId());
                    return "Объявление создано!";
                })
                .exceptionally(throwable -> {
                    boardCache.relayFailed(forumId, throwable);
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
        var forumId = textContent.forumId();
        var appId = requireAppId(context);

        var response = boardCache.relay(forumId)
                .thenCompose(relay -> publishApplication(relay, content, threadId, appId))
                .thenApply(message -> {
                    boardCache.messagesChanged(threadId);
                    log.info("Successfully published application through webhook");
                    return "Сообщение отправлено";
                })
                .exceptionally(throwable -> {
                    boardCache.relayFailed(forumId, throwable);
                    log.error("Failed to publish application through webhook", throwable);
                    throw new RuntimeException("Failed to publish application through webhook", throwable);
                }).join();

        return Variants.of(response);
    }



    @EntityMessageHandler(PREFIX + "editText")
    public VariantValue editText(EntityMessageContext context) {
        var textContent = variantObjectMapper.fromVariant(context.arg(0), TextContent.class);
        var content = textContent.content();
        var messageRef = new DiscordMessageRef(textContent.threadId(), textContent.messageId());
        var forumId = textContent.forumId();
        var response = boardCache.relay(forumId)
                .thenCompose(relay ->
                        editApplication(relay, content, messageRef, requireAppId(context)))
                .thenApply(message -> {
                    boardCache.messageEdited(messageRef);
                    log.info("Successfully edited application through webhook");
                    return "Сообщение изменено";
                })
                .exceptionally(throwable -> {
                    boardCache.relayFailed(forumId, throwable);
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
        var noteContent = variantObjectMapper.fromVariant(context.arg(0), ForumMessagesContent.class);
        var threadId = noteContent.threadId();
        var forumId = noteContent.forumId();
        var starterRef = new DiscordMessageRef(threadId, threadId);
        var appId = requireAppId(context);

        var response = boardCache.relay(forumId)
                .thenCompose(relay -> requireOwnToken(relay, starterRef, appId))
                .thenCompose(token -> discordGateway.deleteForumPost(forumId, threadId))
                .thenApply(ignored -> {
                    boardCache.postDeleted(forumId, threadId);
                    log.info("Successfully deleted forum post {}", threadId);
                    return "Объявление удалено";
                })
                .exceptionally(throwable -> {
                    boardCache.relayFailed(forumId, throwable);
                    log.error("Failed to delete forum post {}", threadId, throwable);
                    throw new RuntimeException("Failed to delete forum post", throwable);
                }).join();

        return Variants.of(response);
    }

    @EntityMessageHandler(PREFIX + "deleteText")
    public VariantValue deleteText(EntityMessageContext context) {
        var textContent = variantObjectMapper.fromVariant(context.arg(0), TextContent.class);
        var messageRef = new DiscordMessageRef(textContent.threadId(), textContent.messageId());
        var forumId = textContent.forumId();
        if (messageRef.messageId() == messageRef.channelId()) {
            throw new IllegalStateException("Первое сообщение нельзя удалить отдельно, удалите объявление целиком.");
        }
        var appId = requireAppId(context);

        var response = boardCache.relay(forumId)
                .thenCompose(relay -> requireOwnToken(relay, messageRef, appId)
                        .thenCompose(token -> discordGateway.deleteWebhookMessage(relay, messageRef)))
                .thenApply(ignored -> {
                    boardCache.messagesChanged(messageRef.channelId());
                    log.info("Successfully deleted application message through webhook");
                    return "Сообщение удалено";
                })
                .exceptionally(throwable -> {
                    boardCache.relayFailed(forumId, throwable);
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
