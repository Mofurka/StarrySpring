package irden.space.proxy.plugin.site;

import irden.space.proxy.plugin.player_manager.events.PlayerConnectedEvent;
import irden.space.proxy.plugin.player_manager.model.Player;
import irden.space.proxy.plugin.site.persistence.repository.PlayerAttributesRecordJdbcRepository;
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
    private final PlayerAttributesRecordJdbcRepository repository;


    @Async
    @EventListener
    public void onPlayerConnectedEvent(PlayerConnectedEvent event) {
        Player player = event.player();
        repository.findByPlayerUuid(player.uuid().toString()).ifPresentOrElse(
                record -> {
                    Map<String, Object> metadata = player.metadata();
                    metadata.put("appId", record.appId());
                    metadata.put("discordId", record.discordId());
                },
                () -> log.info("{} does not have the connection record",  player.uuid()));
    }


}
