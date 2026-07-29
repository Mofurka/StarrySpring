package irden.space.proxy.plugin.player_manager;

import irden.space.proxy.plugin.player_manager.model.Player;

import java.util.List;

public interface PlayerRegistry<T> {

    boolean add(String id, T player);

    T getBySessionId(String sessionId);

    Player getByUuid(String uuid);

    Player getByEntityId(int entityId);

    Player getByClientId(int clientId);

    T removeBySessionId(String sessionId);

    int size();

    List<T> getAll();

}
