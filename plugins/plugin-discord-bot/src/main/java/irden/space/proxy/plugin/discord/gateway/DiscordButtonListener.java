package irden.space.proxy.plugin.discord.gateway;

import irden.space.proxy.plugin.discord.api.*;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class DiscordButtonListener extends ListenerAdapter implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(DiscordButtonListener.class);

    private final Map<String, DiscordButtonHandler> handlers = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public DiscordButtonRegistration register(String buttonId, DiscordButtonHandler handler) {
        Objects.requireNonNull(buttonId, "buttonId");
        Objects.requireNonNull(handler, "handler");

        DiscordButtonHandler previous = handlers.put(buttonId, handler);
        if (previous != null) {
            log.warn("Discord button handler for '{}' was replaced", buttonId);
        }

        return () -> handlers.remove(buttonId, handler);
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();
        DiscordButtonHandler handler = handlers.get(buttonId);

        if (handler == null) {
            log.warn("No handler registered for Discord button '{}'", buttonId);
            event.reply("Эта кнопка больше не работает. СООБЩИТЕ АДМИНУ ПОЖАЛУЙСТА!.").setEphemeral(true).queue();
            return;
        }

        event.deferReply(true).queue(
                ignored -> executor.execute(() -> dispatch(event, buttonId, handler)),
                error -> log.warn("Failed to acknowledge Discord button '{}'", buttonId, error)
        );
    }

    private void dispatch(ButtonInteractionEvent event, String buttonId, DiscordButtonHandler handler) {
        ButtonContext context = new ButtonContext(event, buttonId);

        try {
            handler.handle(context);
        } catch (RuntimeException e) {
            log.error("Discord button handler '{}' failed", buttonId, e);
            context.replyQuietly("При обработке нажатия произошла ошибка.");
            return;
        }

        if (!context.replied()) {
            context.replyQuietly("Готово.");
        }
    }

    @Override
    public void destroy() {
        executor.shutdown();
        handlers.clear();
    }

    private static final class ButtonContext implements DiscordButtonContext {

        private final ButtonInteractionEvent event;
        private final String buttonId;
        private final AtomicBoolean replied = new AtomicBoolean();

        private ButtonContext(ButtonInteractionEvent event, String buttonId) {
            this.event = event;
            this.buttonId = buttonId;
        }

        @Override
        public String buttonId() {
            return buttonId;
        }

        @Override
        public String userId() {
            return event.getUser().getId();
        }

        @Override
        public String userName() {
            return event.getMember() != null
                    ? event.getMember().getEffectiveName()
                    : event.getUser().getName();
        }

        @Override
        public long channelId() {
            return event.getChannel().getIdLong();
        }

        @Override
        public DiscordMessageRef message() {
            return new DiscordMessageRef(channelId(), event.getMessageIdLong());
        }

        @Override
        public CompletableFuture<Void> reply(String content) {
            return reply(DiscordMessage.text(content));
        }

        @Override
        public CompletableFuture<Void> reply(DiscordMessage message) {
            Objects.requireNonNull(message, "message");
            replied.set(true);

            return event.getHook()
                    .sendMessage(DiscordMessageMapper.toCreateData(message))
                    .submit()
                    .thenAccept(sent -> {
                    });
        }

        private boolean replied() {
            return replied.get();
        }

        private void replyQuietly(String content) {
            reply(content).exceptionally(failure -> {
                log.warn("Failed to answer Discord button '{}'", buttonId, failure);
                return null;
            });
        }
    }
}
