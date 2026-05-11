package irden.space.proxy.plugin.player_manager.api;

import irden.space.proxy.plugin.player_manager.PlayerManagerPlugin;
import irden.space.proxy.plugin.player_manager.model.Player;

import java.util.List;
import java.util.Optional;

public final class DefaultPlayerManagerApi implements PlayerManagerApi {

    private final PlayerManagerPlugin plugin;

    public DefaultPlayerManagerApi(PlayerManagerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Optional<Player> findPlayer(String identifier, boolean loggedIn) {
        return plugin.findPlayer(identifier, loggedIn);
    }

    @Override
    public List<Player> searchPlayers(String prefix, int limit, boolean loggedIn) {
        return plugin.searchPlayers(prefix, limit, loggedIn);
    }

    @Override
    public List<Player> findAllPlayersByIpAddress(String ipAddress) {
        return plugin.findAllPlayersByIpAddress(ipAddress);
    }

    @Override
    public Optional<Player> getPlayerBySessionId(String sessionId) {
        return plugin.getPlayerBySessionId(sessionId);
    }
}