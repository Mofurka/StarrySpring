package irden.space.proxy.plugin.player_manager.api;

import irden.space.proxy.plugin.player_manager.PlayerDirectory;
import irden.space.proxy.plugin.player_manager.model.Player;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public final class DefaultPlayerManagerApi implements PlayerManagerApi {

    private final PlayerDirectory playerDirectory;

    public DefaultPlayerManagerApi(PlayerDirectory playerDirectory) {
        this.playerDirectory = playerDirectory;
    }

    @Override
    public Optional<Player> findPlayer(String identifier, boolean loggedIn) {
        return playerDirectory.findPlayer(identifier, loggedIn);
    }

    @Override
    public List<Player> searchPlayers(String prefix, int limit, boolean loggedIn) {
        return playerDirectory.searchPlayers(prefix, limit, loggedIn);
    }

    @Override
    public List<Player> findAllPlayersByIpAddress(String ipAddress) {
        return playerDirectory.findAllPlayersByIpAddress(ipAddress);
    }

    @Override
    public Optional<Player> getPlayerBySessionId(String sessionId) {
        return playerDirectory.getPlayerBySessionId(sessionId);
    }
}
