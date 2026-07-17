package irden.space.proxy.plugin.site;

import irden.space.proxy.plugin.player_manager.model.Player;
import irden.space.proxy.plugin.site.web.client.IrdenAppClient;
import irden.space.proxy.plugin.site.web.client.dto.LinkPlayerRequest;
import irden.space.proxy.plugin.site.web.client.dto.LinkPlayerResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SiteLinker {
    private final IrdenAppClient irdenAppClient;

    public boolean link(Player player, String secret) {
        LinkPlayerRequest build = LinkPlayerRequest.builder()
                .uuid(player.uuid().toString())
                .name(player.name())
                .secret(secret)
                .build();
        LinkPlayerResponse linkResult = irdenAppClient.link(build);
        if (linkResult.discordId() != null && linkResult.applicationId() != null) {
            log.info("Персонаж был привязан!");
            log.info("Персонаж был привязан!");
            log.info("Персонаж был привязан!");
            return true;
        }
        return false;
    }

    public boolean unlink(Player player) {
        irdenAppClient.unlink(player.uuid().toString());
        return true;
    }
}
