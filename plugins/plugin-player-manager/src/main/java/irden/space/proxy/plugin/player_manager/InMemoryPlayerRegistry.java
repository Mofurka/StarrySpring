package irden.space.proxy.plugin.player_manager;


import irden.space.proxy.plugin.player_manager.model.Player;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryPlayerRegistry implements PlayerRegistry<Player> {
    private final Map<String, Player> playersBySessionId = new ConcurrentHashMap<>();

    @Override
    public boolean add(String id, Player player) {
        if (playersBySessionId.containsKey(player.sessionContext().sessionId())) {
            return false; // Player with the same session ID already exists
        }
        playersBySessionId.put(player.sessionContext().sessionId(), player);
        return true;
    }

    @Override
    public Player getBySessionId(String sessionId) {
        return playersBySessionId.get(sessionId);
    }

    @Override
    public Player removeBySessionId(String sessionId) {
        return playersBySessionId.remove(sessionId);
    }

    @Override
    public int size() {
        return playersBySessionId.size();
    }

    @Override
    public List<Player> getAll() {
        return List.copyOf(playersBySessionId.values());
    }

    public boolean updatePlayer(Player player) {
        if (!playersBySessionId.containsKey(player.sessionContext().sessionId())) {
            return false; // Player with the given session ID does not exist
        }
        playersBySessionId.put(player.sessionContext().sessionId(), player);
        return true;
    }
}
