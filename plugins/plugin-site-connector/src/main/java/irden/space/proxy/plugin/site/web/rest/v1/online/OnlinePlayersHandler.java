package irden.space.proxy.plugin.site.web.rest.v1.online;

import irden.space.proxy.plugin.player_manager.api.PlayerManagerApi;
import irden.space.proxy.plugin.player_manager.model.Player;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OnlinePlayersHandler {

    private final PlayerManagerApi playerManagerApi;


    public Collection<OnlinePlayerInfoDto> handleRequest(boolean pub) {

        List<Player> players = playerManagerApi.onlinePlayers();

        if (!players.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            return players.stream().map(player -> new OnlinePlayerInfoDto(
                            pub ? null : player.uuid().toString(),
                            pub ? null : player.name(),
                            player.nickname(),
                            Duration.between(player.lastSeen(), now).toSeconds()
                    )
            ).toList();
        }
        return Collections.emptyList();
    }


}
