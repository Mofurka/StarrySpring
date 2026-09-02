package irden.space.proxy.plugin.discord.api;

public record DiscordAttachment(
        long id,
        String fileName,
        String url,
        String proxyUrl,
        String contentType,
        int size,
        boolean image,
        boolean video,
        boolean spoiler
) {
}
