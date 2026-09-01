package irden.space.proxy.plugin.irden;

import irden.space.proxy.plugin.general.permissions.ChatPermissions;
import irden.space.proxy.plugin.irden.integration.persistence.repository.PlayerAttributesRepository;
import irden.space.proxy.plugin.irden.persistence.model.account.AccountEntity;
import irden.space.proxy.plugin.irden.service.AccountTransactionService;
import irden.space.proxy.plugin.irden.service.PlayerAccountService;
import irden.space.proxy.plugin.irden.service.exception.AccountNotFoundException;
import irden.space.proxy.plugin.player_manager.PlayerAccessService;
import irden.space.proxy.plugin.player_manager.events.PlayerConnectedEvent;
import irden.space.proxy.plugin.player_manager.model.Player;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static irden.space.proxy.plugin.irden.constants.PlayerAccountDefaults.PLAYER_METADATA_ACCOUNT_KEY;

@Component
@RequiredArgsConstructor
@Slf4j
public class PlayerDataResolver {
    private final PlayerAttributesRepository repository;
    private final PlayerAccessService playerAccessService;
    private final PlayerAccountService playerAccountService;
    private final AccountTransactionService accountTransactionService;

    @Async
    @Order(0)
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
                    initPlayerAccount(player, r.getApplicationId());
                    log.debug("{} is linked to application {}, chat granted for this session",
                            player.uuid(), r.getApplicationId());
                },
                () -> {
                    log.info("{} does not have the connection record", player.uuid());
                    // вынести потом в проперти из хардкода
                    if (player.account().equals("player")) {
                        player.sendMessage("Привяжите персонажа к сайту. Для этого зайдите на сайт, в выпадающем списке у имени персонажа \"Связи с персонажами\"" );
                    }
                });
    }


    private void initPlayerAccount(Player player, Long applicationId) {
        try {
            AccountEntity account = playerAccountService.getMainAccount(player.uuid());
            player.metadata().put(PLAYER_METADATA_ACCOUNT_KEY, account.getId());
        } catch (AccountNotFoundException _) {
            log.info("Player {} account does not exist. Init one", player.uuid());
            AccountEntity newAccount = playerAccountService.initPlayerMainAccount(applicationId.toString());
            accountTransactionService.deposit(newAccount.getId(), initFirstMoney(), UUID.randomUUID(), "Account created");
            player.metadata().put(PLAYER_METADATA_ACCOUNT_KEY, newAccount.getId());
        }
    }

    //random 50-100
    @SuppressWarnings("java:S2140")
    private Long initFirstMoney() {
        return (long) (Math.random() * 51) + 50;
    }

}
