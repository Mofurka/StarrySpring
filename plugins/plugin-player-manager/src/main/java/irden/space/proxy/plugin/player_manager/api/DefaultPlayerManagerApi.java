package irden.space.proxy.plugin.player_manager.api;

import irden.space.proxy.plugin.player_manager.persistence.PlayerJdbcRepository;
import irden.space.proxy.plugin.player_manager.persistence.model.PlayerRecord;

import java.util.Optional;

public class DefaultPlayerManagerApi implements PlayerManagerApi {

    private final PlayerJdbcRepository repository;

    public DefaultPlayerManagerApi(PlayerJdbcRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(PlayerRecord player) {
        repository.save(player);
    }

    @Override
    public Optional<PlayerRecord> findByUuid(String uuid) {
        return repository.findByUuid(uuid);
    }
}