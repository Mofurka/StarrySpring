package irden.space.proxy.plugin.player_manager;

import irden.space.proxy.plugin.player_manager.model.TempPlayer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryConnectingPlayers implements PlayerRegistry<TempPlayer> {
    private final Map<String, TempPlayer> playersBySessionId = new ConcurrentHashMap<>();
    @Override
    public boolean add(String id, TempPlayer player) {
        if (playersBySessionId.containsKey(player.sessionId())) {
            return false; // Player with the same session ID already exists
        }
        playersBySessionId.put(player.sessionId(), player);
        return true;
    }

    @Override
    public TempPlayer getBySessionId(String sessionId) {
        return playersBySessionId.get(sessionId);
    }

    @Override
    public TempPlayer removeBySessionId(String sessionId) {
        return playersBySessionId.remove(sessionId);
    }

    @Override
    public int size() {
        return playersBySessionId.size();
    }

    @Override
    public List<TempPlayer> getAll() {
        return List.copyOf(playersBySessionId.values());
    }
}
