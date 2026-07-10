package irden.space.proxy.plugin.player_manager.events;

import irden.space.proxy.plugin.player_manager.model.Player;

public record PlayerConnectedEvent(String sessionId, Player player) {
}
