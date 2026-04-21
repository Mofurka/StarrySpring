package irden.space.proxy.application.player;

import irden.space.proxy.application.port.out.PlayerRepository;
import irden.space.proxy.domain.player.Player;
import irden.space.proxy.domain.player.PlayerId;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class PlayerCommandService {
    private final PlayerRepository playerRepository;

    public PlayerCommandService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    public Player create(String username) {
        OffsetDateTime now = OffsetDateTime.now();
        Player player = new Player(
                PlayerId.generate(),
                username,
                now,
                now
        );
        return playerRepository.save(player);
    }
}