package irden.space.proxy.plugin.irden.integration.web.rest.v1.player_balance;

import irden.space.proxy.plugin.irden.integration.web.dto.player_app_id.PlayerAppIdParam;
import irden.space.proxy.plugin.irden.persistence.model.account.AccountEntity;
import irden.space.proxy.plugin.irden.service.AccountTransactionService;
import irden.space.proxy.plugin.irden.service.PlayerAccountService;
import irden.space.proxy.plugin.irden.service.exception.AccountNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Больше нет
 */
@Component
@RequiredArgsConstructor
public class PlayerBalanceHandler {
    private final PlayerAccountService playerAccountService;
    private final AccountTransactionService accountTransactionService;

    public long handleGetPlayerBalance(PlayerAppIdParam param) {
        AccountEntity account;
        try {
            account = requireAccount(param);
        } catch (AccountNotFoundException _) {
            return 0L;
        }
        return account.getBalance();
    }


    public long handleResetPlayerBalance(PlayerAppIdParam param) {
        AccountEntity account = requireAccount(param);
        if (account.getBalance() == 0) {
            return 0L;
        }
        var transaction = accountTransactionService.withdraw(account.getId(), account.getBalance(), UUID.randomUUID(), "Обнуление баланса с сайта");
        return transaction.getToBalanceAfter();
    }


    public long setPlayerBalance(PlayerAppIdParam param, BalanceRequestBody balanceRequestBody) {
        AccountEntity account = requireAccount(param);
        long amount = account.getBalance() + balanceRequestBody.amount();
        if (account.getBalance() == amount) {
            return amount;
        }
        if (amount < 0) {
            return handleResetPlayerBalance(param);
        } else if (account.getBalance() < amount) {
            return handlePlayerBalanceDeposit(account, balanceRequestBody.amount(), balanceRequestBody.fromAppId());
        } else {
            return handlePlayerBalanceWithdraw(account, balanceRequestBody.amount(), balanceRequestBody.fromAppId());
        }

    }

    public long updatePlayerBalance(PlayerAppIdParam param, BalanceRequestBody balanceRequestBody) {
        AccountEntity account = requireAccount(param);
        if (balanceRequestBody.amount() >= 0) {
            return handlePlayerBalanceDeposit(account, balanceRequestBody.amount(), balanceRequestBody.fromAppId());
        } else {
            return handlePlayerBalanceWithdraw(account, balanceRequestBody.amount(), balanceRequestBody.fromAppId());
        }
    }

    private AccountEntity requireAccount(PlayerAppIdParam param) throws AccountNotFoundException {
        return playerAccountService.getMainAccount(param.toString());
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
