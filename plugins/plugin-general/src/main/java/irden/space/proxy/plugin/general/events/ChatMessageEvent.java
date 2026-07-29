package irden.space.proxy.plugin.general.events;

import irden.space.proxy.plugin.player_manager.model.Player;

import java.util.Map;

public record ChatMessageEvent(
        Player sender,
        String mode,
        String message,
        Map<String, Object> metadata
) {
    public ChatMessageEvent(Player sender, String mode, String message) {
        this(sender, mode, message, null);
    }
}
