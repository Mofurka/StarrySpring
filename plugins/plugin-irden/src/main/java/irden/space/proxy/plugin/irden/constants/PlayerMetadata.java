package irden.space.proxy.plugin.irden.constants;

import irden.space.proxy.plugin.player_manager.model.Player;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class PlayerMetadata {
    public static final String APPLICATION_ID = "applicationId";
    public static final String DISCORD_ID = "discordId";

    public static Long getApplicationId(Player player) {
        Object o = player.metadata().get(APPLICATION_ID);
        if (o != null) {
            return (Long) o;
        }
        throw new IllegalStateException("Персонаж не привязан");
    }

    public static Long getDiscordId(Player player) {
        Object o = player.metadata().get(DISCORD_ID);
        if (o != null) {
            return (Long) o;
        }
        throw new IllegalStateException("Персонаж не привязан");
    }

}
