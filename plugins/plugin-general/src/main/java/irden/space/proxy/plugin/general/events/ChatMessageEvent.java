package irden.space.proxy.plugin.general.events;

import irden.space.proxy.plugin.player_manager.model.Player;
import lombok.Builder;

import java.util.Map;

@Builder
public record ChatMessageEvent(
        Player sender,
        String mode,
        String message,
        boolean silent,
        Map<String, Object> metadata
) {
    public ChatMessageEvent(Player sender, String mode, String message) {
        this(sender, mode, message, false, null);
    }

    public ChatMessageEvent(Player player, String string, String text, Map<String, Object> proximityRadius) {
        this(player, string, text, false, proximityRadius);
    }
}
