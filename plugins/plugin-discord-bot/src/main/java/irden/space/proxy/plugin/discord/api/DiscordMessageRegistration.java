package irden.space.proxy.plugin.discord.api;

public interface DiscordMessageRegistration extends AutoCloseable {

    @Override
    void close();
}
