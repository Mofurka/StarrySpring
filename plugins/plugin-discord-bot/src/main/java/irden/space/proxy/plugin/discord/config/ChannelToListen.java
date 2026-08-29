package irden.space.proxy.plugin.discord.config;

public record ChannelToListen(
        long channelId,
        String nameOverride,
        String modeOverride,
        String authorNameOverride
    ) {}