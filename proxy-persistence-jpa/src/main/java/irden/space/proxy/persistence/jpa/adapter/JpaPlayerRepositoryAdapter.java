package irden.space.proxy.persistence.jpa.adapter;

import irden.space.proxy.application.port.out.PlayerRepository;
import irden.space.proxy.domain.player.Player;
import irden.space.proxy.domain.player.PlayerId;
import irden.space.proxy.persistence.jpa.mapper.PlayerMapper;
import irden.space.proxy.persistence.jpa.repository.SpringDataPlayerRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public class JpaPlayerRepositoryAdapter implements PlayerRepository {

    private final SpringDataPlayerRepository repository;

    public JpaPlayerRepositoryAdapter(SpringDataPlayerRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Player> findById(PlayerId id) {
        return repository.findById(id.value()).map(PlayerMapper::toDomain);
    }

    @Override
    public Optional<Player> findByUsername(String username) {
        return repository.findByUsername(username).map(PlayerMapper::toDomain);
    }

    @Override
    @Transactional
    public Player save(Player player) {
        return PlayerMapper.toDomain(
                repository.save(PlayerMapper.toEntity(player))
        );
    }
}