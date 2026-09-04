package irden.space.proxy.plugin.discord.api;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface DiscordGateway {

    Duration DEFAULT_READY_TIMEOUT = Duration.ofSeconds(60);

    boolean isReady();

    CompletableFuture<Void> whenReady(Duration timeout);

    default CompletableFuture<Void> whenReady() {
        return whenReady(DEFAULT_READY_TIMEOUT);
    }

    CompletableFuture<DiscordMessageRef> send(long channelId, DiscordMessage message);

    CompletableFuture<DiscordMessageRef> edit(DiscordMessageRef message, DiscordMessage content);

    CompletableFuture<Void> delete(DiscordMessageRef message);

    /**
     * Уискорда лимит на изменение канала два раза в 10 минут, поэтому если
     */
    CompletableFuture<Void> setChannelName(long channelId, String channelName);

    CompletableFuture<Void> deleteLastMessage(long channelId);

    /**
     * <pre>{@code
     * @PostConstruct
     * void registerButtons() {
     *     discord.onButton("server:ping", this::onPingPressed);
     * }
     * }</pre>
     */
    DiscordButtonRegistration onButton(String buttonId, DiscordButtonHandler handler);

    CompletableFuture<List<DiscordReceivedMessage>> history(long channelId, DiscordHistoryQuery query);


    CompletableFuture<Optional<DiscordReceivedMessage>> fetchMessage(long channelId, long messageId);


    CompletableFuture<List<DiscordReceivedMessage>> pinnedMessages(long channelId);

    CompletableFuture<List<DiscordForumChannel>> forumChannels();

    CompletableFuture<List<DiscordForumChannel>> forumChannels(long guildId);

    CompletableFuture<Optional<DiscordForumChannel>> forumChannel(long forumChannelId);

    CompletableFuture<List<DiscordForumPost>> forumPosts(long forumChannelId, DiscordForumPostQuery query);

    CompletableFuture<Optional<DiscordForumPost>> forumPost(long postId);

    CompletableFuture<Optional<DiscordForumPost>> forumPost(long forumChannelId, long postId);

    CompletableFuture<DiscordForumPost> createForumPost(long forumChannelId, DiscordForumPostRequest request);

    CompletableFuture<List<DiscordWebhook>> webhooks(long channelId);

    CompletableFuture<List<DiscordWebhook>> guildWebhooks(long guildId);

    CompletableFuture<Optional<DiscordWebhook>> webhook(long webhookId);

    CompletableFuture<DiscordWebhook> createWebhook(long channelId, String name);

    CompletableFuture<Void> renameWebhook(long webhookId, String name);

    CompletableFuture<Void> setWebhookAvatar(long webhookId, byte[] avatar);

    CompletableFuture<Void> moveWebhook(long webhookId, long channelId);

    CompletableFuture<Void> deleteWebhook(long webhookId);

    CompletableFuture<DiscordMessageRef> sendWebhook(DiscordWebhookTarget target, DiscordWebhookMessage message);

    CompletableFuture<DiscordMessageRef> editWebhookMessage(
            DiscordWebhookTarget target,
            long messageId,
            DiscordWebhookMessage content
    );

    CompletableFuture<DiscordMessageRef> editWebhookMessage(
            DiscordWebhookTarget target,
            DiscordMessageRef message,
            DiscordMessage content
    );

    CompletableFuture<Void> deleteWebhookMessage(DiscordWebhookTarget target, long messageId, long threadId);

    CompletableFuture<Void> deleteWebhookMessage(DiscordWebhookTarget target, DiscordMessageRef message);

    CompletableFuture<Optional<DiscordReceivedMessage>> fetchWebhookMessage(
            DiscordWebhookTarget target,
            long messageId,
            long threadId
    );

    CompletableFuture<Optional<DiscordReceivedMessage>> fetchWebhookMessage(
            DiscordWebhookTarget target,
            DiscordMessageRef message
    );

    /**
     * <pre>{@code
     * @PostConstruct
     * void listenLore() {
     *     registration = discord.onMessage(loreChannelId, this::portToGame);
     * }
     * }</pre>
     */
    DiscordMessageRegistration onMessage(long channelId, DiscordMessageHandler handler);

    DiscordMessageRegistration onAnyMessage(DiscordMessageHandler handler);

    default CompletableFuture<List<DiscordReceivedMessage>> history(long channelId) {
        return history(channelId, DiscordHistoryQuery.latest(DiscordHistoryQuery.MAX_LIMIT));
    }

    default CompletableFuture<List<DiscordReceivedMessage>> history(long channelId, int limit) {
        return history(channelId, DiscordHistoryQuery.latest(limit));
    }

    default CompletableFuture<Optional<DiscordReceivedMessage>> fetchMessage(DiscordMessageRef message) {
        return fetchMessage(message.channelId(), message.messageId());
    }

    default CompletableFuture<Optional<DiscordReceivedMessage>> lastMessage(long channelId) {
        return history(channelId, DiscordHistoryQuery.latest(1))
                .thenApply(messages -> messages.isEmpty() ? Optional.empty() : Optional.of(messages.getFirst()));
    }

    default CompletableFuture<List<DiscordReceivedMessage>> historyWhenReady(
            long channelId,
            DiscordHistoryQuery query,
            Duration timeout
    ) {
        return whenReady(timeout).thenCompose(ignored -> history(channelId, query));
    }

    default CompletableFuture<List<DiscordReceivedMessage>> historyWhenReady(
            long channelId,
            DiscordHistoryQuery query
    ) {
        return historyWhenReady(channelId, query, DEFAULT_READY_TIMEOUT);
    }

    default CompletableFuture<DiscordMessageRef> send(long channelId, String content) {
        return send(channelId, DiscordMessage.text(content));
    }

    default CompletableFuture<DiscordMessageRef> send(long channelId, DiscordEmbed embed) {
        return send(channelId, DiscordMessage.of(embed));
    }


    default CompletableFuture<DiscordMessageRef> sendWhenReady(
            long channelId,
            DiscordMessage message,
            Duration timeout
    ) {
        return whenReady(timeout).thenCompose(ignored -> send(channelId, message));
    }

    default CompletableFuture<DiscordMessageRef> sendWhenReady(long channelId, DiscordMessage message) {
        return sendWhenReady(channelId, message, DEFAULT_READY_TIMEOUT);
    }

    default CompletableFuture<DiscordMessageRef> sendWhenReady(long channelId, String content) {
        return sendWhenReady(channelId, DiscordMessage.text(content));
    }

    default CompletableFuture<DiscordMessageRef> sendWhenReady(long channelId, DiscordEmbed embed) {
        return sendWhenReady(channelId, DiscordMessage.of(embed));
    }

    default CompletableFuture<List<DiscordForumPost>> forumPosts(long forumChannelId) {
        return forumPosts(forumChannelId, DiscordForumPostQuery.active());
    }

    default CompletableFuture<List<DiscordForumPost>> forumPosts(long forumChannelId, int limit) {
        return forumPosts(forumChannelId, DiscordForumPostQuery.active(limit));
    }

    default CompletableFuture<Optional<DiscordForumChannel>> forumChannelByName(String name) {
        return forumChannels().thenApply(forums -> forums.stream()
                .filter(forum -> forum.name().equalsIgnoreCase(name))
                .findFirst());
    }

    default CompletableFuture<Optional<DiscordReceivedMessage>> forumPostStarter(long postId) {
        return fetchMessage(postId, postId);
    }

    default CompletableFuture<List<DiscordForumChannel>> forumChannelsWhenReady(Duration timeout) {
        return whenReady(timeout).thenCompose(ignored -> forumChannels());
    }

    default CompletableFuture<List<DiscordForumChannel>> forumChannelsWhenReady() {
        return forumChannelsWhenReady(DEFAULT_READY_TIMEOUT);
    }

    default CompletableFuture<List<DiscordForumPost>> forumPostsWhenReady(
            long forumChannelId,
            DiscordForumPostQuery query,
            Duration timeout
    ) {
        return whenReady(timeout).thenCompose(ignored -> forumPosts(forumChannelId, query));
    }

    default CompletableFuture<List<DiscordForumPost>> forumPostsWhenReady(
            long forumChannelId,
            DiscordForumPostQuery query
    ) {
        return forumPostsWhenReady(forumChannelId, query, DEFAULT_READY_TIMEOUT);
    }

    default CompletableFuture<DiscordForumPost> createForumPost(long forumChannelId, String name, String content) {
        return createForumPost(forumChannelId, DiscordForumPostRequest.of(name, content));
    }

    default CompletableFuture<DiscordForumPost> createForumPost(long forumChannelId, String name, DiscordEmbed embed) {
        return createForumPost(forumChannelId, DiscordForumPostRequest.of(name, embed));
    }

    default CompletableFuture<Optional<DiscordWebhook>> webhookByName(long channelId, String name) {
        return webhooks(channelId).thenApply(existing -> existing.stream()
                .filter(webhook -> webhook.named(name))
                .findFirst());
    }

    default CompletableFuture<DiscordWebhook> webhookOrCreate(long channelId, String name) {
        return webhooks(channelId).thenCompose(existing -> existing.stream()
                .filter(webhook -> webhook.executable() && webhook.named(name))
                .findFirst()
                .map(CompletableFuture::completedFuture)
                .orElseGet(() -> createWebhook(channelId, name)));
    }

    default CompletableFuture<DiscordMessageRef> sendWebhook(DiscordWebhookTarget target, String content) {
        return sendWebhook(target, DiscordWebhookMessage.of(content));
    }

    default CompletableFuture<DiscordMessageRef> sendWebhook(
            DiscordWebhookTarget target,
            String username,
            String content
    ) {
        return sendWebhook(target, DiscordWebhookMessage.as(username, content));
    }

    default CompletableFuture<DiscordMessageRef> sendWebhookWhenReady(
            DiscordWebhookTarget target,
            DiscordWebhookMessage message,
            Duration timeout
    ) {
        return whenReady(timeout).thenCompose(ignored -> sendWebhook(target, message));
    }

    default CompletableFuture<DiscordMessageRef> sendWebhookWhenReady(
            DiscordWebhookTarget target,
            DiscordWebhookMessage message
    ) {
        return sendWebhookWhenReady(target, message, DEFAULT_READY_TIMEOUT);
    }

    default CompletableFuture<DiscordMessageRef> editWebhookMessage(
            DiscordWebhookTarget target,
            long messageId,
            DiscordMessage content
    ) {
        return editWebhookMessage(target, messageId, DiscordWebhookMessage.of(content));
    }

    default CompletableFuture<Void> deleteWebhookMessage(DiscordWebhookTarget target, long messageId) {
        return deleteWebhookMessage(target, messageId, 0);
    }

    default CompletableFuture<Optional<DiscordReceivedMessage>> fetchWebhookMessage(
            DiscordWebhookTarget target,
            long messageId
    ) {
        return fetchWebhookMessage(target, messageId, 0);
    }

    default CompletableFuture<DiscordMessageRef> publishWebhook(
            DiscordMessageRef previous,
            DiscordWebhookTarget target,
            DiscordWebhookMessage message
    ) {
        if (previous == null) {
            return sendWebhook(target, message);
        }
        return editWebhookMessage(target, previous.messageId(), message)
                .exceptionallyCompose(_ -> sendWebhook(target, message));
    }

    default CompletableFuture<DiscordMessageRef> publish(
            DiscordMessageRef previous,
            long channelId,
            DiscordMessage message
    ) {
        if (previous == null) {
            return send(channelId, message);
        }
        return edit(previous, message).exceptionallyCompose(_ -> send(channelId, message));
    }
}
