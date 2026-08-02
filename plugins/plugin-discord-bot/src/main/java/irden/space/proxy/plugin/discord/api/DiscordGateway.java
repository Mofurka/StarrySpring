package irden.space.proxy.plugin.discord.api;

import java.time.Duration;
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
