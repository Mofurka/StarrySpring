package irden.space.proxy.plugin.discord.api;

public record DiscordForumTag(
        long id,
        String name,
        String emoji,
        boolean moderated,
        int position
) {

    public DiscordForumTag {
        name = name == null ? "" : name;
    }

    public boolean hasEmoji() {
        return emoji != null && !emoji.isBlank();
    }

    public boolean named(String other) {
        return other != null && name.equalsIgnoreCase(other);
    }
}
