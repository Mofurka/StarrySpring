package irden.space.proxy.plugin.irden.integration.web.rest.v1.player_balance;

import irden.space.proxy.plugin.irden.integration.web.rest.v1.dto.player_uuid.PlayerUuidParam;
import irden.space.proxy.plugin.irden.persistence.model.account.AccountEntity;
import irden.space.proxy.plugin.irden.service.AccountTransactionService;
import irden.space.proxy.plugin.irden.service.PlayerAccountService;
import irden.space.proxy.plugin.irden.service.exception.AccountNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Путь остался на uuid персонажа, чтобы не ломать контракт с сайтом, но счёт
 * принадлежит привязке: uuid переводится в {@code applicationId} внутри
 * {@link PlayerAccountService}.
 */
@Component
@RequiredArgsConstructor
public class PlayerBalanceHandler {
    private final PlayerAccountService playerAccountService;
    private final AccountTransactionService accountTransactionService;

    public long handleGetPlayerBalance(PlayerUuidParam param) {
        AccountEntity account;
        try {
            account = requireAccount(param);
        } catch (AccountNotFoundException _) {
            return 0L;
        }
        return account.getBalance();
    }


    public long handleResetPlayerBalance(PlayerUuidParam param) {
        AccountEntity account = requireAccount(param);
        if (account.getBalance() == 0) {
            return 0L;
        }
        var transaction = accountTransactionService.withdraw(account.getId(), account.getBalance(), UUID.randomUUID(), "Обнуление баланса с сайта");
        return transaction.getToBalanceAfter();
    }


    public long setPlayerBalance(PlayerUuidParam param, BalanceRequestBody balanceRequestBody) {
        AccountEntity account = requireAccount(param);
        long amount = account.getBalance() + balanceRequestBody.amount();
        if (account.getBalance() == amount) {
            return amount;
        }
        if (amount < 0) {
            return handleResetPlayerBalance(param);
        } else if (account.getBalance() < amount) {
            return handlePlayerBalanceDeposit(account, balanceRequestBody.amount(), balanceRequestBody.fromId());
        } else {
            return handlePlayerBalanceWithdraw(account, balanceRequestBody.amount(), balanceRequestBody.fromId());
        }

    }

    public long updatePlayerBalance(PlayerUuidParam param, BalanceRequestBody balanceRequestBody) {
        AccountEntity account = requireAccount(param);
        if (balanceRequestBody.amount() >= 0) {
            return handlePlayerBalanceDeposit(account, balanceRequestBody.amount(), balanceRequestBody.fromId());
        } else {
            return handlePlayerBalanceWithdraw(account, balanceRequestBody.amount(), balanceRequestBody.fromId());
        }
    }

    private AccountEntity requireAccount(PlayerUuidParam param) throws AccountNotFoundException {
        return playerAccountService.getMainAccount(param.uuid());
    }

    private long handlePlayerBalanceDeposit(AccountEntity account, long amount, String fromId) {
        String message = fromId != null ? "Пополнеие баланса от " + fromId : "Пополнеие баланса с сайта";
        var transaction = accountTransactionService.deposit(account.getId(), amount, UUID.randomUUID(), message);
        return transaction.getToBalanceAfter();
    }


    private long handlePlayerBalanceWithdraw(AccountEntity account, long amount, String fromId) {
        String message = fromId != null ? "Списание баланса от " + fromId : "Списание баланса с сайта";
        var transaction = accountTransactionService.withdraw(account.getId(), amount, UUID.randomUUID(), message);
        return transaction.getToBalanceAfter();
    }

}
