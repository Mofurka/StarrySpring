package irden.space.proxy.plugin.irden.integration;

import irden.space.proxy.plugin.general.permissions.ChatPermissions;
import irden.space.proxy.plugin.irden.integration.persistence.repository.PlayerAttributesRepository;
import irden.space.proxy.plugin.player_manager.PlayerAccessService;
import irden.space.proxy.plugin.player_manager.events.PlayerConnectedEvent;
import irden.space.proxy.plugin.player_manager.model.Player;
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
    private final PlayerAccessService playerAccessService;

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
                    metadata.put("discordIdLink", "<@%s>".formatted(r.getDiscordId()));

                    playerAccessService.grantSessionPermissions(
                            player.sessionContext().sessionId(),
                            ChatPermissions.SEND_MESSAGE
                    );
                    log.debug("{} is linked to application {}, chat granted for this session",
                            player.uuid(), r.getApplicationId());
                },
                () -> log.info("{} does not have the connection record", player.uuid()));
    }
}
