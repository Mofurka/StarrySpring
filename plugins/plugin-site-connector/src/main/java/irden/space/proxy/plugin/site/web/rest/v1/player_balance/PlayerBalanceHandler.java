package irden.space.proxy.plugin.site.web.rest.v1.player_balance;

import irden.space.proxy.plugin.irden.persistence.model.AccountEntity;
import irden.space.proxy.plugin.irden.persistence.model.AccountOwnerType;
import irden.space.proxy.plugin.irden.service.AccountService;
import irden.space.proxy.plugin.irden.service.AccountTransactionService;
import irden.space.proxy.plugin.site.web.rest.v1.dto.player_uuid.PlayerUuidParam;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static irden.space.proxy.plugin.irden.constants.PlayerAccountDefaults.PLAYER_DEFAULT_ACCOUNT_CODE;

@Component
@RequiredArgsConstructor
public class PlayerBalanceHandler {
    private final AccountService accountService;
    private final AccountTransactionService accountTransactionService;

    public long handleGetPlayerBalance(PlayerUuidParam param) {
        String uuid = param.uuid();
        AccountEntity account;
        try {
            account = accountService.getAccount(AccountOwnerType.CHARACTER, uuid, PLAYER_DEFAULT_ACCOUNT_CODE);
        } catch (IllegalArgumentException _) {
            return 0L;
        }
        return account.getBalance();
    }


    public long handleResetPlayerBalance(PlayerUuidParam param) {
        String uuid = param.uuid();
        var account = accountService.getAccount(AccountOwnerType.CHARACTER, uuid, PLAYER_DEFAULT_ACCOUNT_CODE);
        if (account.getBalance() == 0) {
            return 0L;
        }
        var transaction = accountTransactionService.withdraw(account.getId(), account.getBalance(), UUID.randomUUID(), "Обнуление баланса с сайта");
        return transaction.getToBalanceAfter();
    }


    public long setPlayerBalance(PlayerUuidParam param, BalanceRequestBody balanceRequestBody) {
        String uuid = param.uuid();
        var account = accountService.getAccount(AccountOwnerType.CHARACTER, uuid, PLAYER_DEFAULT_ACCOUNT_CODE);
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
        String uuid = param.uuid();
        var account = accountService.getAccount(AccountOwnerType.CHARACTER, uuid, PLAYER_DEFAULT_ACCOUNT_CODE);
        if (balanceRequestBody.amount() >= 0) {
            return handlePlayerBalanceDeposit(account, balanceRequestBody.amount(), balanceRequestBody.fromId());
        } else {
            return handlePlayerBalanceWithdraw(account, balanceRequestBody.amount(), balanceRequestBody.fromId());
        }
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
