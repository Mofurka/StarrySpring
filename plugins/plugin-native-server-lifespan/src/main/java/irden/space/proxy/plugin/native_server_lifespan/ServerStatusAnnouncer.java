package irden.space.proxy.plugin.native_server_lifespan;

import irden.space.proxy.plugin.discord.api.*;
import irden.space.proxy.plugin.native_server_lifespan.model.response.NativeServerInfo;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServerStatusAnnouncer {

    private static final int COLOR_ONLINE = 0x57F287;
    private static final int COLOR_OFFLINE = 0xED4245;
    private static final String PING_BUTTON_ID = "native-server-lifespan:ping";

    private final ServerLifespan serverLifespan;
    private final DiscordGateway discord;
    private final NativeServerLifespanConfig config;

    private final AtomicBoolean staleMessageCleaned = new AtomicBoolean();
    private volatile DiscordMessageRef panel;

    @Scheduled(fixedDelay = 30000)
    public void refresh() {
        Long channelId = config.discordAnnounceChannelId();
        if (channelId == null) {
            return;
        }

        discord.whenReady()
                .thenCompose(ignored -> cleanupStaleMessage(channelId))
                .thenCompose(ignored -> publishPanel(channelId))
                .exceptionally(failure -> {
                    log.warn("Failed to refresh Discord server status panel", failure);
                    return null;
                });
    }


    private CompletableFuture<Void> cleanupStaleMessage(long channelId) {
        if (!staleMessageCleaned.compareAndSet(false, true)) {
            return CompletableFuture.completedFuture(null);
        }
        return discord.deleteLastMessage(channelId);
    }

    @PostConstruct
    void registerButtons() {
        discord.onButton(PING_BUTTON_ID, this::onPingPressed);
    }

    private void onPingPressed(DiscordButtonContext context) {
        NativeServerInfo serverInfo = serverLifespan.getServerInfo();
        Instant startedAt = serverInfo.startedAt();

        String answer = serverInfo.running()
                ? "Сервер отвечает: запущен <t:%d:R>.".formatted(
                startedAt == null ? Instant.now().getEpochSecond() : startedAt.getEpochSecond())
                : "Сервер не запущен.";

        context.reply(answer);
    }

    private CompletableFuture<Void> publishPanel(long channelId) {
        NativeServerInfo serverInfo = serverLifespan.getServerInfo();

        DiscordMessage message = DiscordMessage.builder()
                .embed(statusEmbed(serverInfo))
                .button(DiscordButton.primary(PING_BUTTON_ID, "Пинг"))
                .build();

        return discord.publish(panel, channelId, message)
                .thenAccept(ref -> panel = ref);
    }

    private DiscordEmbed statusEmbed(NativeServerInfo serverInfo) {
        Instant startedAt = serverInfo.startedAt();

        return DiscordEmbed.builder()
                .title("Состояние сервера")
                .color(serverInfo.running() ? COLOR_ONLINE : COLOR_OFFLINE)
                .field("Онлайн", serverInfo.running() ? "да" : "нет", true)
                .field("Время жизни", startedAt == null ? "—" : "<t:%d:R>".formatted(startedAt.getEpochSecond()), true)
                .field("Последнее обновление статуса:", "<t:%d:R>".formatted(Instant.now().getEpochSecond()), true)
                .footerText("Если последнее обновление статуса больше 1 минуты, значит сервер лежит, либо дискорд бот не работает.")
                .timestamp(Instant.now())
                .build();
    }
}
