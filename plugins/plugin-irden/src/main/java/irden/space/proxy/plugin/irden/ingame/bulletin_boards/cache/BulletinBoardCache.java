package irden.space.proxy.plugin.irden.ingame.bulletin_boards.cache;

import irden.space.proxy.plugin.discord.api.*;
import irden.space.proxy.plugin.irden.ingame.bulletin_boards.model.response.StarboundForumMessage;
import irden.space.proxy.plugin.irden.ingame.bulletin_boards.model.response.StarboundForumPost;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static irden.space.proxy.plugin.irden.test.DiscordApplicationTokenService.clearToken;
import static irden.space.proxy.plugin.irden.test.DiscordApplicationTokenService.extractToken;

@Component
public class BulletinBoardCache {

    private static final String RELAY_NAME = "game-relay";
    private static final int ARCHIVED_POSTS_LIMIT = 100;

    private final DiscordGateway discordGateway;
    private final RefreshingCache<Long, List<StarboundForumPost>> archivedPosts;
    private final RefreshingCache<Long, List<StarboundForumMessage>> threadMessages;
    private final RefreshingCache<DiscordMessageRef, Optional<String>> messageContents;
    private final RefreshingCache<Long, DiscordWebhookTarget> relays;
    private DiscordMessageRegistration messageRegistration;

    public BulletinBoardCache(DiscordGateway discordGateway) {
        this.discordGateway = discordGateway;
        this.archivedPosts = new RefreshingCache<>(
                "archived posts", Duration.ofSeconds(30), Duration.ofMinutes(5), this::loadArchivedPosts);
        this.threadMessages = new RefreshingCache<>(
                "thread messages", Duration.ofSeconds(15), Duration.ofMinutes(2), this::loadThreadMessages);
        this.messageContents = new RefreshingCache<>(
                "message contents", Duration.ofSeconds(30), Duration.ofMinutes(5), this::loadMessageContent);
        this.relays = new RefreshingCache<>(
                RELAY_NAME, Duration.ofHours(12), Duration.ofHours(12), this::loadRelay);
    }

    private static List<StarboundForumPost> toPosts(List<DiscordForumPost> posts) {
        return posts.stream()
                .map(post -> StarboundForumPost.builder()
                        .id(post.id())
                        .name(post.name())
                        .archived(post.archived())
                        .locked(post.locked())
                        .createdAt(post.createdAt().toEpochMilli())
                        .messageCount(post.messageCount())
                        .starterRef(post.starterRef())
                        .build())
                .toList();
    }

    private static StarboundForumMessage toMessage(DiscordReceivedMessage message) {
        String content = extractToken(message.content()).isPresent()
                ? clearToken(message.content())
                : message.content();

        return StarboundForumMessage.builder()
                .id(message.messageId())
                .author(message.author().name())
                .content(content)
                .createdAt(message.createdAt().toEpochMilli() / 1000)
                .build();
    }

    @PostConstruct
    void listenDiscordMessages() {
        messageRegistration = discordGateway.onAnyMessage(message -> threadMessages.reload(message.channelId()));
    }

    @PreDestroy
    void stopListening() {
        if (messageRegistration != null) {
            messageRegistration.close();
        }
    }

    public CompletableFuture<List<StarboundForumPost>> posts(long forumId, boolean archived) {
        if (archived) {
            return archivedPosts.get(forumId);
        }
        return discordGateway.forumPosts(forumId, DiscordForumPostQuery.active())
                .thenApply(BulletinBoardCache::toPosts);
    }

    public CompletableFuture<List<StarboundForumMessage>> messages(long threadId) {
        return threadMessages.get(threadId);
    }

    public CompletableFuture<Optional<String>> content(DiscordMessageRef message) {
        return messageContents.get(message);
    }

    public CompletableFuture<DiscordWebhookTarget> relay(long forumId) {
        return relays.get(forumId);
    }

    public void messagesChanged(long threadId) {
        threadMessages.reload(threadId);
    }

    public void messageEdited(DiscordMessageRef message) {
        threadMessages.reload(message.channelId());
        messageContents.reload(message);
    }

    public void postDeleted(long forumId, long threadId) {
        threadMessages.invalidate(threadId);
        messageContents.invalidate(new DiscordMessageRef(threadId, threadId));
        archivedPosts.reload(forumId);
    }

    public void relayFailed(long forumId, Throwable failure) {
        Throwable cause = failure instanceof CompletionException && failure.getCause() != null
                ? failure.getCause()
                : failure;

        if (cause instanceof IllegalStateException || cause instanceof IllegalArgumentException) {
            return;
        }
        relays.invalidate(forumId);
    }

    private CompletableFuture<List<StarboundForumPost>> loadArchivedPosts(long forumId) {
        return discordGateway.forumPosts(forumId, DiscordForumPostQuery.archived(ARCHIVED_POSTS_LIMIT))
                .thenApply(BulletinBoardCache::toPosts);
    }

    private CompletableFuture<List<StarboundForumMessage>> loadThreadMessages(long threadId) {
        return discordGateway.history(threadId)
                .thenApply(messages -> messages.stream()
                        .map(BulletinBoardCache::toMessage)
                        .toList()
                        .reversed());
    }

    private CompletableFuture<Optional<String>> loadMessageContent(DiscordMessageRef message) {
        return discordGateway.fetchMessage(message)
                .thenApply(received -> received.map(DiscordReceivedMessage::content));
    }

    private CompletableFuture<DiscordWebhookTarget> loadRelay(long forumId) {
        return discordGateway.webhookOrCreate(forumId, RELAY_NAME)
                .thenApply(DiscordWebhook::requireTarget);
    }
}
