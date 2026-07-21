package irden.space.proxy.plugin.irden;

import irden.space.proxy.plugin.command_handler.ChatCommand;
import irden.space.proxy.plugin.command_handler.CommandContext;
import irden.space.proxy.plugin.command_handler.CommandSpec;
import irden.space.proxy.plugin.command_handler.IntegerArgumentType;
import irden.space.proxy.plugin.command_handler.wording.RussianLiteralsUtils;
import irden.space.proxy.plugin.irden.constants.PlayerAccountDefaults;
import irden.space.proxy.plugin.irden.persistence.model.AccountEntity;
import irden.space.proxy.plugin.irden.service.AccountService;
import irden.space.proxy.plugin.irden.service.AccountTransactionService;
import irden.space.proxy.plugin.player_manager.api.PlayerManagerApi;
import irden.space.proxy.plugin.player_manager.command.PlayerOnlineTargetArgumentType;
import irden.space.proxy.plugin.player_manager.command.PlayerTarget;
import irden.space.proxy.plugin.player_manager.model.Player;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlayerBalanceCommandsHandler {
    private final AccountService accountService;
    private final AccountTransactionService accountTransactionService;


    public void handleMoneyGiveCommand(CommandContext context) {
        Player sender = context.sender(Player.class).get();
        Player recipient = context.get("player", PlayerTarget.class).player();
        int amount = context.get("amount", Integer.class);
        UUID senderAccountId = (UUID) sender.metadata().get(PlayerAccountDefaults.PLAYER_METADATA_ACCOUNT_KEY);
        UUID recipientAccountId = (UUID) recipient.metadata().get(PlayerAccountDefaults.PLAYER_METADATA_ACCOUNT_KEY);
        var transaction = accountTransactionService.transfer(senderAccountId, recipientAccountId, amount, sender.uuid().toJavaUuid(), "Тестовая транзакция");
        context.reply("Передано %s %s %s", transaction.getAmount(), getDeclinedAmount(transaction.getAmount()), recipient.name());
        recipient.sendMessage("%s передал вам %s %s", recipient.nickname(), transaction.getAmount(), getDeclinedAmount(transaction.getAmount()));

    }

    private String getDeclinedAmount(long amount) {
        return RussianLiteralsUtils.declineWord((int) amount, "монета", "монеты", "монет");
    }

    public void handleMoneyCommand(CommandContext context) {
        context.sender(Player.class).ifPresent(player -> {
            AccountEntity account = accountService.getAccount((UUID) player.metadata().get(PlayerAccountDefaults.PLAYER_METADATA_ACCOUNT_KEY));
            long balance = account.getBalance();
            String declinedWord = getDeclinedAmount(balance);
            context.reply("У вас %s %s", account.getBalance(), declinedWord);
        });
    }

}
