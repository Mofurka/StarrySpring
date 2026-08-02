package irden.space.proxy.plugin.discord.gateway;

import irden.space.proxy.plugin.discord.api.DiscordUnavailableException;
import irden.space.proxy.plugin.discord.config.DiscordBotConfiguration;
import irden.space.proxy.plugin.discord.proxy.DiscordProxySupport;
import irden.space.proxy.plugin.discord.proxy.Socks5Proxy;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class DiscordConnection {

    private static final Logger log = LoggerFactory.getLogger(DiscordConnection.class);

    private final DiscordBotConfiguration configuration;

    private volatile JDA jda;
    private volatile CompletableFuture<JDA> ready = new CompletableFuture<>();


    public synchronized boolean start() {
        if (jda != null) {
            return true;
        }

        String token = configuration.botToken();
        if (token == null || token.isBlank()) {
            log.info("Discord bot token is not set; Discord bot will not start");
            failReady("Discord bot token is not set");
            return false;
        }

        JDABuilder builder = JDABuilder.createDefault(token, EnumSet.allOf(GatewayIntent.class));
        applyProxy(builder);

        JDA connected;
        try {
            connected = builder.build().awaitReady();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            failReady("Discord bot startup was interrupted");
            throw new IllegalStateException("Failed to initialize Discord bot", e);
        } catch (RuntimeException e) {
            failReady("Discord bot failed to start: " + e.getMessage());
            throw e;
        }

        jda = connected;
        ready.completeAsync(() -> connected);

        return true;
    }


    public CompletableFuture<JDA> ready() {
        return ready;
    }

    public boolean isReady() {
        JDA current = jda;
        return current != null
                && current.getStatus() != JDA.Status.SHUTTING_DOWN
                && current.getStatus() != JDA.Status.SHUTDOWN;
    }

    public Optional<JDA> jda() {
        return Optional.ofNullable(jda);
    }

    public JDA require() {
        JDA current = jda;
        if (current == null) {
            throw new DiscordUnavailableException("Discord bot is not started");
        }
        return current;
    }

    public synchronized void shutdown() {
        JDA current = jda;
        jda = null;

        resetReady("Discord bot is shutting down");

        if (current == null) {
            return;
        }

        log.info("Shutting down Discord bot");
        current.shutdown();
    }


    private void failReady(String reason) {
        ready.completeExceptionally(new DiscordUnavailableException(reason));
    }


    private void resetReady(String reason) {
        CompletableFuture<JDA> pending = ready;
        ready = new CompletableFuture<>();
        pending.completeExceptionally(new DiscordUnavailableException(reason));
    }

    private void applyProxy(JDABuilder builder) {
        DiscordBotConfiguration.Proxy proxySettings = configuration.proxy();
        if (proxySettings == null || !proxySettings.isEnabled()) {
            return;
        }

        Socks5Proxy proxy = Socks5Proxy.parse(proxySettings.proxyUrl());
        DiscordProxySupport.apply(builder, proxy);

        log.info("Discord bot will connect through {}", proxy);
    }
}
