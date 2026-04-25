package irden.space.proxy.plugin.player_manager.api;

import irden.space.proxy.plugin.player_manager.persistence.model.PlayerRecord;

import java.util.Optional;

public interface PlayerManagerApi {
    void save(PlayerRecord player);
    Optional<PlayerRecord> findByUuid(String uuid);
}