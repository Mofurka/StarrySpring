package irden.space.proxy.plugin.player_manager.api;

import irden.space.proxy.plugin.player_manager.model.Player;

import java.util.List;
import java.util.Optional;

public interface PlayerManagerApi {

    List<Player> onlinePlayers();

    Optional<Player> findPlayer(String identifier, boolean loggedIn);

    // Only online player have the client and entity id.
    Optional<Player> findByClientId(int clientId);

    Optional<Player> findByEntityId(int entityId);

    Optional<Player> findPlayerByUuid(String uuid, boolean loggedIn);

    List<Player> searchPlayers(String prefix, int limit, boolean loggedIn);

    List<Player> findAllPlayersByIpAddress(String ipAddress);

    Optional<Player> findPlayerBySessionId(String sessionId);
}