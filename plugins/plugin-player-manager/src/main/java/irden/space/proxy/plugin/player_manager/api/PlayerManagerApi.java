package irden.space.proxy.plugin.player_manager.api;

import irden.space.proxy.plugin.player_manager.persistence.PlayerRecord;

import java.util.Optional;
import java.util.UUID;

public interface PlayerManagerApi {
    void save(PlayerRecord player);
    Optional<PlayerRecord> findByUuid(UUID uuid);
}