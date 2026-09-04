package irden.space.proxy.plugin.discord.gateway;

import irden.space.proxy.plugin.discord.api.*;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.attribute.IWebhookContainer;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.restaction.*;
import net.dv8tion.jda.api.requests.restaction.pagination.MessagePaginationAction;
import net.dv8tion.jda.api.requests.restaction.pagination.PaginationAction;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class JdaDiscordGateway implements DiscordGateway {

    private static final Set<ErrorResponse> MISSING = EnumSet.of(
            ErrorResponse.UNKNOWN_MESSAGE,
            ErrorResponse.UNKNOWN_CHANNEL,
            ErrorResponse.UNKNOWN_WEBHOOK);

    private final DiscordConnection connection;
    private final DiscordButtonListener buttonListener;
    private final DiscordMessageListener messageListener;

    private static Throwable readyFailure(Throwable failure, Duration timeout) {
        Throwable cause = failure instanceof CompletionException && failure.getCause() != null
                ? failure.getCause()
                : failure;

        return cause instanceof TimeoutException
                ? new DiscordUnavailableException("Discord bot was not ready within " + timeout)
                : cause;
    }

    private static <T> CompletableFuture<T> call(Supplier<CompletableFuture<T>> action) {
        try {
            return action.get();
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private static CompletableFuture<List<Message>> retrieveHistory(
            MessageChannel channel,
            DiscordHistoryQuery query
    ) {
        return switch (query.anchor()) {
            case LATEST -> paginate(channel, query.limit(), PaginationAction.PaginationOrder.BACKWARD, 0);
            case BEFORE -> paginate(
                    channel, query.limit(), PaginationAction.PaginationOrder.BACKWARD, query.anchorMessageId());
            case AFTER -> paginate(
                    channel, query.limit(), PaginationAction.PaginationOrder.FORWARD, query.anchorMessageId());
            case BEGINNING -> paginate(channel, query.limit(), PaginationAction.PaginationOrder.FORWARD, 0);
            case AROUND -> channel.getHistoryAround(query.anchorMessageId(), query.limit())
                    .submit()
                    .thenApply(MessageHistory::getRetrievedHistory);
        };
    }

    private static CompletableFuture<List<Message>> paginate(
            MessageChannel channel,
            int limit,
            PaginationAction.PaginationOrder order,
            long anchorMessageId
    ) {
        MessagePaginationAction action = channel.getIterableHistory().order(order);
        if (anchorMessageId > 0) {
            action = action.skipTo(anchorMessageId);
        }
        return action.takeAsync(limit);
    }

    /**
     * JDA отдаёт историю в порядке обхода: BACKWARD - от свежих к старым, FORWARD - наоборот.
     * Приводим её к порядку, который просил вызывающий.
     */
    private static List<DiscordReceivedMessage> toOrderedHistory(
            List<Message> messages,
            DiscordHistoryQuery query
    ) {
        List<DiscordReceivedMessage> mapped = new ArrayList<>(DiscordMessageMapper.toReceived(messages));
        Comparator<DiscordReceivedMessage> byId = Comparator.comparingLong(DiscordReceivedMessage::messageId);
        mapped.sort(query.oldestFirst() ? byId : byId.reversed());
        return List.copyOf(mapped);
    }


    private static List<DiscordForumPost> mergePosts(
            List<DiscordForumPost> active,
            List<DiscordForumPost> archived,
            DiscordForumPostQuery query
    ) {
        Map<Long, DiscordForumPost> byId = new LinkedHashMap<>(active.size() + archived.size());
        for (DiscordForumPost post : active) {
            if (query.matches(post)) {
                byId.putIfAbsent(post.id(), post);
            }
        }
        for (DiscordForumPost post : archived) {
            if (query.matches(post)) {
                byId.putIfAbsent(post.id(), post);
            }
        }

        List<DiscordForumPost> merged = new ArrayList<>(byId.values());
        Comparator<DiscordForumPost> byCreation = Comparator.comparingLong(DiscordForumPost::id);
        merged.sort(query.oldestFirst() ? byCreation : byCreation.reversed());

        return merged.size() <= query.limit()
                ? List.copyOf(merged)
                : List.copyOf(merged.subList(0, query.limit()));
    }


    private static CompletableFuture<Optional<DiscordForumPost>> findArchivedPost(ForumChannel forum, long postId) {
        AtomicReference<ThreadChannel> found = new AtomicReference<>();

        return forum.retrieveArchivedPublicThreadChannels()
                .forEachAsync(thread -> {
                    if (thread.getIdLong() != postId) {
                        return true;
                    }
                    found.set(thread);
                    return false;
                })
                .thenApply(ignored -> Optional.ofNullable(found.get()).map(DiscordForumMapper::toPost));
    }


    private static <T> CompletableFuture<Optional<T>> emptyWhenMissing(Throwable failure) {
        Throwable cause = failure instanceof CompletionException && failure.getCause() != null
                ? failure.getCause()
                : failure;

        if (cause instanceof ErrorResponseException error && MISSING.contains(error.getErrorResponse())) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        return CompletableFuture.failedFuture(cause);
    }

    private static DiscordMessageRef toRef(Message message) {
        return new DiscordMessageRef(message.getChannelIdLong(), message.getIdLong());
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @Override
    public DiscordButtonRegistration onButton(String buttonId, DiscordButtonHandler handler) {
        return buttonListener.register(buttonId, handler);
    }

    @Override
    public DiscordMessageRegistration onMessage(long channelId, DiscordMessageHandler handler) {
        return messageListener.register(channelId, handler);
    }

    @Override
    public DiscordMessageRegistration onAnyMessage(DiscordMessageHandler handler) {
        return messageListener.registerForAnyChannel(handler);
    }

    @Override
    public CompletableFuture<List<DiscordReceivedMessage>> history(long channelId, DiscordHistoryQuery query) {
        Objects.requireNonNull(query, "query");

        return call(() -> retrieveHistory(channel(channelId), query)
                .thenApply(messages -> toOrderedHistory(messages, query)));
    }

    @Override
    public CompletableFuture<Optional<DiscordReceivedMessage>> fetchMessage(long channelId, long messageId) {
        return call(() -> channel(channelId)
                .retrieveMessageById(messageId)
                .submit()
                .thenApply(message -> Optional.of(DiscordMessageMapper.toReceived(message)))
                .exceptionallyCompose(JdaDiscordGateway::emptyWhenMissing));
    }

    @Override
    public CompletableFuture<List<DiscordReceivedMessage>> pinnedMessages(long channelId) {
        return call(() -> channel(channelId)
                .retrievePinnedMessages()
                .takeAsync(DiscordHistoryQuery.MAX_PAGE_SIZE)
                .thenApply(pinned -> {
                    List<Message> messages = new ArrayList<>(pinned.size());
                    pinned.forEach(entry -> messages.add(entry.getMessage()));
                    return DiscordMessageMapper.toReceived(messages);
                }));
    }

    @Override
    public CompletableFuture<List<DiscordForumChannel>> forumChannels() {
        return call(() -> CompletableFuture.completedFuture(
                List.copyOf(DiscordForumMapper.toForumChannels(connection.require().getForumChannels()))));
    }

    @Override
    public CompletableFuture<List<DiscordForumChannel>> forumChannels(long guildId) {
        return call(() -> CompletableFuture.completedFuture(
                List.copyOf(DiscordForumMapper.toForumChannels(guild(guildId).getForumChannels()))));
    }

    @Override
    public CompletableFuture<Optional<DiscordForumChannel>> forumChannel(long forumChannelId) {
        return call(() -> {
            ForumChannel forum = connection.require().getChannelById(ForumChannel.class, forumChannelId);
            return CompletableFuture.completedFuture(
                    forum == null ? Optional.empty() : Optional.of(DiscordForumMapper.toForumChannel(forum)));
        });
    }

    @Override
    public CompletableFuture<List<DiscordForumPost>> forumPosts(long forumChannelId, DiscordForumPostQuery query) {
        Objects.requireNonNull(query, "query");

        return call(() -> {
            ForumChannel forum = forum(forumChannelId);

            List<DiscordForumPost> active = query.scope().includesActive()
                    ? DiscordForumMapper.toPosts(forum.getThreadChannels()).stream()
                    .filter(post -> !post.archived())
                    .toList()
                    : List.of();

            CompletableFuture<List<DiscordForumPost>> archived = query.scope().includesArchived()
                    ? forum.retrieveArchivedPublicThreadChannels()
                    .takeAsync(query.limit())
                    .thenApply(DiscordForumMapper::toPosts)
                    : CompletableFuture.completedFuture(List.of());

            return archived.thenApply(archivedPosts -> mergePosts(active, archivedPosts, query));
        });
    }

    @Override
    public CompletableFuture<Optional<DiscordForumPost>> forumPost(long postId) {
        return call(() -> {
            ThreadChannel thread = connection.require().getChannelById(ThreadChannel.class, postId);
            return CompletableFuture.completedFuture(
                    thread == null ? Optional.empty() : Optional.of(DiscordForumMapper.toPost(thread)));
        });
    }

    @Override
    public CompletableFuture<Optional<DiscordForumPost>> forumPost(long forumChannelId, long postId) {
        return call(() -> {
            ForumChannel forum = forum(forumChannelId);

            ThreadChannel cached = connection.require().getChannelById(ThreadChannel.class, postId);
            if (cached != null && cached.getParentChannel().getIdLong() == forumChannelId) {
                return CompletableFuture.completedFuture(Optional.of(DiscordForumMapper.toPost(cached)));
            }

            return findArchivedPost(forum, postId);
        });
    }

    @Override
    public CompletableFuture<DiscordForumPost> createForumPost(long forumChannelId, DiscordForumPostRequest request) {
        Objects.requireNonNull(request, "request");

        return call(() -> {
            ForumPostAction action = forum(forumChannelId)
                    .createForumPost(request.name(), DiscordMessageMapper.toCreateData(request.message()));

            if (!request.tagIds().isEmpty()) {
                action = action.setTags(DiscordForumMapper.toTagSnowflakes(request.tagIds()));
            }

            return action.submit().thenApply(post -> DiscordForumMapper.toPost(post.getThreadChannel()));
        });
    }

    @Override
    public CompletableFuture<List<DiscordWebhook>> webhooks(long channelId) {
        return call(() -> webhookContainer(channelId)
                .retrieveWebhooks()
                .submit()
                .thenApply(DiscordWebhookMapper::toWebhooks));
    }

    @Override
    public CompletableFuture<List<DiscordWebhook>> guildWebhooks(long guildId) {
        return call(() -> guild(guildId)
                .retrieveWebhooks()
                .submit()
                .thenApply(DiscordWebhookMapper::toWebhooks));
    }

    @Override
    public CompletableFuture<Optional<DiscordWebhook>> webhook(long webhookId) {
        return call(() -> retrieveWebhook(webhookId)
                .thenApply(webhook -> Optional.of(DiscordWebhookMapper.toWebhook(webhook)))
                .exceptionallyCompose(JdaDiscordGateway::emptyWhenMissing));
    }

    @Override
    public CompletableFuture<DiscordWebhook> createWebhook(long channelId, String name) {
        Objects.requireNonNull(name, "name");

        return call(() -> webhookContainer(channelId)
                .createWebhook(name)
                .submit()
                .thenApply(DiscordWebhookMapper::toWebhook));
    }

    @Override
    public CompletableFuture<Void> renameWebhook(long webhookId, String name) {
        Objects.requireNonNull(name, "name");

        return call(() -> retrieveWebhook(webhookId)
                .thenCompose(webhook -> webhook.getManager().setName(name).submit()));
    }

    @Override
    public CompletableFuture<Void> setWebhookAvatar(long webhookId, byte[] avatar) {
        return call(() -> retrieveWebhook(webhookId)
                .thenCompose(webhook -> webhook.getManager()
                        .setAvatar(avatar == null ? null : DiscordWebhookMapper.toIcon(avatar))
                        .submit()));
    }

    @Override
    public CompletableFuture<Void> moveWebhook(long webhookId, long channelId) {
        return call(() -> {
            TextChannel channel = connection.require().getChannelById(TextChannel.class, channelId);
            if (channel == null) {
                throw new DiscordUnavailableException(
                        "Discord text channel " + channelId + " is unknown or unavailable");
            }

            return retrieveWebhook(webhookId)
                    .thenCompose(webhook -> webhook.getManager().setChannel(channel).submit());
        });
    }

    @Override
    public CompletableFuture<Void> deleteWebhook(long webhookId) {
        return call(() -> retrieveWebhook(webhookId).thenCompose(webhook -> webhook.delete().submit()));
    }

    @Override
    public CompletableFuture<DiscordMessageRef> sendWebhook(
            DiscordWebhookTarget target,
            DiscordWebhookMessage message
    ) {
        Objects.requireNonNull(message, "message");

        return call(() -> {
            WebhookMessageCreateAction<Message> action = webhookClient(target)
                    .sendMessage(DiscordMessageMapper.toCreateData(message.message()));

            if (hasText(message.username())) {
                action = action.setUsername(message.username());
            }
            if (hasText(message.avatarUrl())) {
                action = action.setAvatarUrl(message.avatarUrl());
            }
            if (message.inThread()) {
                action = action.setThreadId(message.threadId());
            }

            return action.submit().thenApply(JdaDiscordGateway::toRef);
        });
    }

    @Override
    public CompletableFuture<DiscordMessageRef> editWebhookMessage(
            DiscordWebhookTarget target,
            long messageId,
            DiscordWebhookMessage content
    ) {
        Objects.requireNonNull(content, "content");

        return call(() -> {
            WebhookMessageEditAction<Message> action = webhookClient(target)
                    .editMessageById(messageId, DiscordMessageMapper.toEditData(content.message()));

            if (content.inThread()) {
                action = action.setThreadId(content.threadId());
            }

            return action.submit().thenApply(JdaDiscordGateway::toRef);
        });
    }

    @Override
    public CompletableFuture<DiscordMessageRef> editWebhookMessage(
            DiscordWebhookTarget target,
            DiscordMessageRef message,
            DiscordMessage content
    ) {
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(content, "content");

        return call(() -> editWebhookMessage(
                target,
                message.messageId(),
                DiscordWebhookMessage.of(content).inThread(threadOf(message))));
    }

    @Override
    public CompletableFuture<Void> deleteWebhookMessage(DiscordWebhookTarget target, DiscordMessageRef message) {
        Objects.requireNonNull(message, "message");

        return call(() -> deleteWebhookMessage(target, message.messageId(), threadOf(message)));
    }

    @Override
    public CompletableFuture<Optional<DiscordReceivedMessage>> fetchWebhookMessage(
            DiscordWebhookTarget target,
            DiscordMessageRef message
    ) {
        Objects.requireNonNull(message, "message");

        return call(() -> fetchWebhookMessage(target, message.messageId(), threadOf(message)));
    }

    @Override
    public CompletableFuture<Void> deleteWebhookMessage(DiscordWebhookTarget target, long messageId, long threadId) {
        return call(() -> {
            WebhookMessageDeleteAction action = webhookClient(target)
                    .deleteMessageById(messageId);

            if (threadId > 0) {
                action = action.setThreadId(threadId);
            }

            return action.submit();
        });
    }

    @Override
    public CompletableFuture<Optional<DiscordReceivedMessage>> fetchWebhookMessage(
            DiscordWebhookTarget target,
            long messageId,
            long threadId
    ) {
        return call(() -> {
            WebhookMessageRetrieveAction action = webhookClient(target)
                    .retrieveMessageById(Long.toUnsignedString(messageId));

            if (threadId > 0) {
                action = action.setThreadId(threadId);
            }

            return action.submit()
                    .thenApply(message -> Optional.of(DiscordMessageMapper.toReceived(message)))
                    .exceptionallyCompose(JdaDiscordGateway::emptyWhenMissing);
        });
    }

    @Override
    public boolean isReady() {
        return connection.isReady();
    }

    @Override
    public CompletableFuture<Void> whenReady(Duration timeout) {
        Objects.requireNonNull(timeout, "timeout");

        return connection.ready()
                .copy()
                .orTimeout(Math.max(1, timeout.toMillis()), TimeUnit.MILLISECONDS)
                .thenAccept(jda -> {
                })
                .exceptionallyCompose(failure -> CompletableFuture.failedFuture(readyFailure(failure, timeout)));
    }

    @Override
    public CompletableFuture<DiscordMessageRef> send(long channelId, DiscordMessage message) {
        Objects.requireNonNull(message, "message");

        return call(() -> channel(channelId)
                .sendMessage(DiscordMessageMapper.toCreateData(message))
                .submit()
                .thenApply(sent -> new DiscordMessageRef(channelId, sent.getIdLong())));
    }

    @Override
    public CompletableFuture<DiscordMessageRef> edit(DiscordMessageRef message, DiscordMessage content) {
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(content, "content");

        return call(() -> channel(message.channelId())
                .editMessageById(message.messageId(), DiscordMessageMapper.toEditData(content))
                .submit()
                .thenApply(edited -> message));
    }

    @Override
    public CompletableFuture<Void> delete(DiscordMessageRef message) {
        Objects.requireNonNull(message, "message");

        return call(() -> channel(message.channelId())
                .deleteMessageById(message.messageId())
                .submit());
    }

    @Override
    public CompletableFuture<Void> setChannelName(long channelId, String channelName) {
        Objects.requireNonNull(channelName, "channelName");

        return call(() -> {
            GuildChannel channel = connection.require().getChannelById(GuildChannel.class, channelId);
            if (channel == null) {
                throw new DiscordUnavailableException("Discord channel " + channelId + " is unknown or unavailable");
            }
            return channel.getManager().setName(channelName).submit();
        });
    }

    @Override
    public CompletableFuture<Void> deleteLastMessage(long channelId) {
        return call(() -> {
            MessageChannel channel = channel(channelId);
            long selfId = connection.require().getSelfUser().getIdLong();

            return channel.getHistory()
                    .retrievePast(1)
                    .submit()
                    .thenCompose(messages -> {
                        if (messages.isEmpty()) {
                            return CompletableFuture.completedFuture(null);
                        }

                        Message last = messages.getFirst();
                        if (last.getAuthor().getIdLong() != selfId) {
                            return CompletableFuture.completedFuture(null);
                        }

                        return last.delete().submit();
                    });
        });
    }

    private ForumChannel forum(long forumChannelId) {
        ForumChannel forum = connection.require().getChannelById(ForumChannel.class, forumChannelId);
        if (forum == null) {
            throw new DiscordUnavailableException(
                    "Discord forum channel " + forumChannelId + " is unknown or unavailable");
        }
        return forum;
    }

    private Guild guild(long guildId) {
        Guild guild = connection.require().getGuildById(guildId);
        if (guild == null) {
            throw new DiscordUnavailableException("Discord guild " + guildId + " is unknown or unavailable");
        }
        return guild;
    }

    private IWebhookContainer webhookContainer(long channelId) {
        IWebhookContainer channel = connection.require().getChannelById(IWebhookContainer.class, channelId);
        if (channel == null) {
            throw new DiscordUnavailableException(
                    "Discord channel " + channelId + " is unknown or does not support webhooks");
        }
        return channel;
    }

    private long threadOf(DiscordMessageRef message) {
        JDA jda = connection.require();

        if (jda.getChannelById(ThreadChannel.class, message.channelId()) != null) {
            return message.channelId();
        }
        return jda.getChannelById(Channel.class, message.channelId()) == null ? message.channelId() : 0;
    }

    private CompletableFuture<Webhook> retrieveWebhook(long webhookId) {
        return connection.require().retrieveWebhookById(Long.toUnsignedString(webhookId)).submit();
    }

    private WebhookClient<Message> webhookClient(DiscordWebhookTarget target) {
        Objects.requireNonNull(target, "target");

        return WebhookClient.createClient(
                connection.require(), Long.toUnsignedString(target.id()), target.token());
    }

    private MessageChannel channel(long channelId) {
        MessageChannel channel = connection.require().getChannelById(MessageChannel.class, channelId);
        if (channel == null) {
            throw new DiscordUnavailableException("Discord channel " + channelId + " is unknown or unavailable");
        }
        return channel;
    }
}
