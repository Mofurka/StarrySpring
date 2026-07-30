package irden.space.proxy.plugin.irden.integration;

import irden.space.proxy.plugin.irden.integration.persistence.model.PlayerAttributesEntity;
import irden.space.proxy.plugin.irden.integration.persistence.repository.PlayerAttributesRepository;
import irden.space.proxy.plugin.irden.integration.web.client.IrdenAppClient;
import irden.space.proxy.plugin.irden.integration.web.client.dto.LinkPlayerRequest;
import irden.space.proxy.plugin.irden.integration.web.client.dto.LinkPlayerResponse;
import irden.space.proxy.plugin.player_manager.model.Player;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class SiteLinker {
    private final IrdenAppClient irdenAppClient;
    private final PlayerAttributesRepository repository;

    @Transactional
    public boolean link(Player player, String secret) {
        LinkPlayerRequest build = LinkPlayerRequest.builder()
                .uuid(player.uuid().toString())
                .name(player.name())
                .secret(secret)
                .build();
        LinkPlayerResponse linkResult = irdenAppClient.link(build);
        if (linkResult.discordId() != null && linkResult.applicationId() != null) {
            var byPlayerUuid = repository.findByPlayerUuid(player.uuid().toString()).orElse(new PlayerAttributesEntity());
            byPlayerUuid.setPlayerUuid(player.uuid().toString());
            byPlayerUuid.setApplicationId(linkResult.applicationId());
            byPlayerUuid.setDiscordId(linkResult.discordId());
            repository.save(byPlayerUuid);
            return true;
        }
        return false;
    }

    public boolean unlink(Player player) {
        irdenAppClient.unlink(player.uuid().toString());
        repository.findByPlayerUuid(player.uuid().toString()).ifPresent(repository::delete);
        return true;
    }
}
