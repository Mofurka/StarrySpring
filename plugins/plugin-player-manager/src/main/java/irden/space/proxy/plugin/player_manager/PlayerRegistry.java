package irden.space.proxy.plugin.player_manager;

import java.util.List;

public interface PlayerRegistry<T> {

    boolean add(String id, T player);

    T getBySessionId(String sessionId);

    T removeBySessionId(String sessionId);

    int size();

    List<T> getAll();

}
