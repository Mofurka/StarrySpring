package irden.space.proxy.plugin.irden;

import irden.space.proxy.plugin.irden.persistence.model.account.AccountEntity;
import irden.space.proxy.plugin.irden.service.PlayerAccountService;
import irden.space.proxy.plugin.irden.service.exception.AccountNotFoundException;
import irden.space.proxy.plugin.player_manager.events.PlayerConnectedEvent;
import irden.space.proxy.plugin.player_manager.model.Player;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import static irden.space.proxy.plugin.irden.constants.PlayerAccountDefaults.PLAYER_METADATA_ACCOUNT_KEY;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlayerBalanceInitializer {
    private final PlayerAccountService playerAccountService;

    @EventListener
    @Async
    public void playerConnectedEvent(PlayerConnectedEvent event) {
        Player player = event.player();
        try {
            AccountEntity account = playerAccountService.getMainAccount(player.uuid());
            player.metadata().put(PLAYER_METADATA_ACCOUNT_KEY, account.getId());
        } catch (AccountNotFoundException _) {
            log.info("Player {} account does not exist", player.uuid());
        }
    }
}
