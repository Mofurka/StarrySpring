package irden.space.proxy.plugin.discord.api;

import java.util.concurrent.CompletableFuture;


public interface DiscordButtonContext {


    String buttonId();

    String userId();

    String userName();

    long channelId();


    DiscordMessageRef message();

    CompletableFuture<Void> reply(String content);

    CompletableFuture<Void> reply(DiscordMessage message);

    default CompletableFuture<Void> reply(DiscordEmbed embed) {
        return reply(DiscordMessage.of(embed));
    }
}
