package irden.space.proxy.plugin.discord.api;

@FunctionalInterface
public interface DiscordMessageHandler {

    void handle(DiscordReceivedMessage message);
}
