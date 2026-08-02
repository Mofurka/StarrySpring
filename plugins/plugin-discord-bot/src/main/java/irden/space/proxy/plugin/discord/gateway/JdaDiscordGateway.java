package irden.space.proxy.plugin.discord.gateway;

import irden.space.proxy.plugin.discord.api.*;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;
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

    @Override
    public DiscordButtonRegistration onButton(String buttonId, DiscordButtonHandler handler) {
        return buttonListener.register(buttonId, handler);
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
