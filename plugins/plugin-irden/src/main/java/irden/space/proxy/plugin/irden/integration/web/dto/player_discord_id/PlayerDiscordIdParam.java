package irden.space.proxy.plugin.irden.integration.web.dto.player_discord_id;


import org.jspecify.annotations.NonNull;

public record PlayerDiscordIdParam(long discordId) {
    public static final String NAME = "discordId";
    public static final String PATH = "/{" + NAME + "}";


    @Override
    public @NonNull String toString() {
        return Long.toString(discordId);
    }
}