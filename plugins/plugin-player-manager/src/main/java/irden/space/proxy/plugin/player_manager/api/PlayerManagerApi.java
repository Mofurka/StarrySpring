package irden.space.proxy.plugin.player_manager.api;

import irden.space.proxy.plugin.player_manager.model.Player;

import java.util.List;
import java.util.Optional;

public interface PlayerManagerApi {

    Optional<Player> findPlayer(String identifier, boolean loggedIn);

    List<Player> searchPlayers(String prefix, int limit, boolean loggedIn);

    List<Player> findAllPlayersByIpAddress(String ipAddress);

    Optional<Player> getPlayerBySessionId(String sessionId);
}