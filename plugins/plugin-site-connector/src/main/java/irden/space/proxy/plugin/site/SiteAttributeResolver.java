package irden.space.proxy.plugin.site;

import irden.space.proxy.plugin.player_manager.events.PlayerConnectedEvent;
import irden.space.proxy.plugin.player_manager.model.Player;
import irden.space.proxy.plugin.site.persistence.repository.PlayerAttributesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class SiteAttributeResolver {
    private final PlayerAttributesRepository repository;


    @Async
    @EventListener
    public void onPlayerConnectedEvent(PlayerConnectedEvent event) {
        Player player = event.player();
        repository.findByPlayerUuid(player.uuid().toString()).ifPresentOrElse(
                r -> {
                    Map<String, Object> metadata = player.metadata();

                    // i dunno how to better keep this contract between services
                    metadata.put("applicationId", r.getApplicationId());
                    metadata.put("discordId", r.getDiscordId());
                },
                () -> log.info("{} does not have the connection record", player.uuid()));
    }
}
