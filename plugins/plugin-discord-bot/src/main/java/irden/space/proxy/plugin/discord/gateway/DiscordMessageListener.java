package irden.space.proxy.plugin.discord.gateway;

import irden.space.proxy.plugin.discord.api.DiscordMessageHandler;
import irden.space.proxy.plugin.discord.api.DiscordMessageRegistration;
import irden.space.proxy.plugin.discord.api.DiscordReceivedMessage;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class DiscordMessageListener extends ListenerAdapter implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(DiscordMessageListener.class);
    private static final long ANY_CHANNEL = 0L;

    private final Map<Long, List<DiscordMessageHandler>> handlers = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public DiscordMessageRegistration register(long channelId, DiscordMessageHandler handler) {
        Objects.requireNonNull(handler, "handler");

        handlers.computeIfAbsent(channelId, ignored -> new CopyOnWriteArrayList<>()).add(handler);

        return () -> handlers.computeIfPresent(channelId, (ignored, registered) -> {
            registered.remove(handler);
            return registered.isEmpty() ? null : registered;
        });
    }

    public DiscordMessageRegistration registerForAnyChannel(DiscordMessageHandler handler) {
        return register(ANY_CHANNEL, handler);
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().equals(event.getJDA().getSelfUser())) {
            return;
        }

        long channelId = event.getChannel().getIdLong();
        List<DiscordMessageHandler> forChannel = handlers.getOrDefault(channelId, List.of());
        List<DiscordMessageHandler> forAnyChannel = handlers.getOrDefault(ANY_CHANNEL, List.of());
        if (forChannel.isEmpty() && forAnyChannel.isEmpty()) {
            return;
        }

        DiscordReceivedMessage message = DiscordMessageMapper.toReceived(event.getMessage());
        dispatch(forChannel, message);
        dispatch(forAnyChannel, message);
    }

    private void dispatch(List<DiscordMessageHandler> targets, DiscordReceivedMessage message) {
        for (DiscordMessageHandler handler : targets) {
            executor.execute(() -> {
                try {
                    handler.handle(message);
                } catch (RuntimeException e) {
                    log.error("Discord message handler for channel {} failed", message.channelId(), e);
                }
            });
        }
    }

    @Override
    public void destroy() {
        executor.shutdown();
        handlers.clear();
    }
}
