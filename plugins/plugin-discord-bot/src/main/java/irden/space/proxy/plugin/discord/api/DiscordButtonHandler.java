package irden.space.proxy.plugin.discord.api;

@FunctionalInterface
public interface DiscordButtonHandler {

    void handle(DiscordButtonContext context);
}
