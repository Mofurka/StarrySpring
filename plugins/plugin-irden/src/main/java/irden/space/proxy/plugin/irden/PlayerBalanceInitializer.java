package irden.space.proxy.plugin.irden;

import irden.space.proxy.plugin.irden.persistence.model.AccountEntity;
import irden.space.proxy.plugin.irden.persistence.model.AccountOwnerType;
import irden.space.proxy.plugin.irden.service.AccountService;
import irden.space.proxy.plugin.player_manager.events.PlayerConnectedEvent;
import irden.space.proxy.plugin.player_manager.model.Player;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static irden.space.proxy.plugin.irden.constants.PlayerAccountDefaults.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlayerBalanceInitializer {
    private final AccountService accountService;

    @EventListener
    @Async
    public void playerConnectedEvent(PlayerConnectedEvent event) {
        Player player = event.player();
        UUID accountUuid;
        try {
            AccountEntity account = accountService.getAccount(AccountOwnerType.CHARACTER, player.uuid().toString(), PLAYER_DEFAULT_ACCOUNT_CODE);
            accountUuid = account.getId();
        } catch (IllegalArgumentException e) {
            log.info("Player {} does not exist", player.uuid().toString());
            log.info("Creating one");
            AccountEntity account = accountService.createAccount(AccountOwnerType.CHARACTER, player.uuid().toString(), player.name(), PLAYER_DEFAULT_ACCOUNT_CODE);
            log.info("Player account created: {}", account.getStatus());
            accountUuid = account.getId();
        }
        player.metadata().put(PLAYER_METADATA_ACCOUNT_KEY, accountUuid);
    }


}
