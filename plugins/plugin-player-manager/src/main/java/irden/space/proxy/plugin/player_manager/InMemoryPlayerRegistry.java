package irden.space.proxy.plugin.player_manager;


import irden.space.proxy.plugin.player_manager.model.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryPlayerRegistry implements PlayerRegistry {
    private final Map<String, Player> playersBySessionId = new ConcurrentHashMap<>();

    @Override
    public boolean add(Player player) {
        if (playersBySessionId.containsKey(player.sessionContext().sessionId())) {
            return false; // Player with the same session ID already exists
        }
        playersBySessionId.put(player.sessionContext().sessionId(), player);
        return true;
    }
}
