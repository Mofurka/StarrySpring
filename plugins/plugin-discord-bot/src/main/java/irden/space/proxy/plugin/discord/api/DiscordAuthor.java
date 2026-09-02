package irden.space.proxy.plugin.discord.api;

public record DiscordAuthor(
        long id,
        String name,
        String displayName,
        String avatarUrl,
        boolean bot,
        boolean system
) {

    public DiscordAuthor {
        name = name == null ? "" : name;
        displayName = displayName == null || displayName.isBlank() ? name : displayName;
    }

    public String mention() {
        return "<@" + id + ">";
    }
}
