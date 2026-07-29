package irden.space.proxy.plugin.player_manager;


import irden.space.proxy.plugin.player_manager.model.Player;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component("onlinePlayerRegistry")
public final class InMemoryPlayerRegistry implements PlayerRegistry<Player> {
    private final Map<String, Player> playersBySessionId = new ConcurrentHashMap<>();
    private final Map<String, Player> playersByUuid = new ConcurrentHashMap<>();
    private final Map<Integer, Player> playersByClientId = new ConcurrentHashMap<>();
    private final Map<Integer, Player> playersByEntityId = new ConcurrentHashMap<>();

    @Override
    public boolean add(String id, Player player) {
        if (playersBySessionId.containsKey(player.sessionContext().sessionId())) {
            return false; // Player with the same session ID already exists
        }
        playersBySessionId.put(player.sessionContext().sessionId(), player);
        playersByUuid.put(player.uuid().toString(), player);
        playersByClientId.put(player.clientId(), player);
        playersByEntityId.put(player.entityId(), player);
        return true;
    }

    @Override
    public Player getBySessionId(String sessionId) {
        return playersBySessionId.get(sessionId);
    }

    @Override
    public Player getByUuid(String uuid) {
        return playersByUuid.get(uuid);
    }

    @Override
    public Player getByEntityId(int entityId) {
        return playersByEntityId.get(entityId);
    }

    @Override
    public Player getByClientId(int clientId) {
        return playersByClientId.get(clientId);
    }

    @Override
    public Player removeBySessionId(String sessionId) {
        Player remove = playersBySessionId.remove(sessionId);
        if (remove != null) {
            playersByUuid.remove(remove.uuid().toString());
            playersByClientId.remove(remove.clientId());
            playersByEntityId.remove(remove.entityId());
        }
        return remove;
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
        playersByUuid.put(player.uuid().toString(), player);
        playersByClientId.put(player.clientId(), player);
        playersByEntityId.put(player.entityId(), player);

        return true;
    }
}
