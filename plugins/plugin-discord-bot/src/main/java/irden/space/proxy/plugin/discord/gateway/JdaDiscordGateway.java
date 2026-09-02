package irden.space.proxy.plugin.discord.gateway;

import irden.space.proxy.plugin.discord.api.*;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.restaction.pagination.MessagePaginationAction;
import net.dv8tion.jda.api.requests.restaction.pagination.PaginationAction;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class JdaDiscordGateway implements DiscordGateway {

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

    private static CompletableFuture<Optional<DiscordReceivedMessage>> emptyWhenMissing(Throwable failure) {
        Throwable cause = failure instanceof CompletionException && failure.getCause() != null
                ? failure.getCause()
                : failure;

        if (cause instanceof ErrorResponseException error
                && (error.getErrorResponse() == ErrorResponse.UNKNOWN_MESSAGE
                || error.getErrorResponse() == ErrorResponse.UNKNOWN_CHANNEL)) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        return CompletableFuture.failedFuture(cause);
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

    private MessageChannel channel(long channelId) {
        MessageChannel channel = connection.require().getChannelById(MessageChannel.class, channelId);
        if (channel == null) {
            throw new DiscordUnavailableException("Discord channel " + channelId + " is unknown or unavailable");
        }
        return channel;
    }
}
