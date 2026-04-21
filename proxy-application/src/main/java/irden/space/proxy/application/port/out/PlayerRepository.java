package irden.space.proxy.application.port.out;

import irden.space.proxy.domain.player.Player;
import irden.space.proxy.domain.player.PlayerId;

import java.util.Optional;

public interface PlayerRepository {
    Optional<Player> findById(PlayerId id);
    Optional<Player> findByUsername(String username);
    Player save(Player player);
}