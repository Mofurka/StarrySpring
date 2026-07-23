package irden.space.proxy.plugin.irden;

import irden.space.proxy.plugin.command_handler.CommandContext;
import irden.space.proxy.plugin.command_handler.wording.RussianLiteralsUtils;
import irden.space.proxy.plugin.irden.account.StructureAccountTarget;
import irden.space.proxy.plugin.irden.account.StructureAccountType;
import irden.space.proxy.plugin.irden.persistence.model.AccountEntity;
import irden.space.proxy.plugin.irden.service.AccountService;
import irden.space.proxy.plugin.irden.service.AccountTransactionService;
import irden.space.proxy.plugin.irden.service.exception.AccountAlreadyExistsException;
import irden.space.proxy.plugin.irden.service.exception.InsufficientFundsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class StructureAccountCommandsHandler {

    private final AccountService accountService;
    private final AccountTransactionService accountTransactionService;

    public void handleCreate(CommandContext context) {
        StructureAccountType type = context.get("type", StructureAccountType.class);
        String name = context.get("name", String.class).trim();

        try {
            AccountEntity account = accountService.createAccount(
                    type.ownerType(),
                    StructureAccountType.toOwnerId(name),
                    name,
                    type.accountCode()
            );
            context.reply("Создан счёт: %s «%s» (баланс %s %s).",
                    type.displayName(), account.getOwnerName(), account.getBalance(), coins(account.getBalance()));
        } catch (AccountAlreadyExistsException e) {
            context.reply("Счёт %s «%s» уже существует.", type.displayName(), name);
        } catch (IllegalArgumentException e) {
            context.reply("Не удалось создать счёт: %s", e.getMessage());
        }
    }

    public void handleInfo(CommandContext context) {
        StructureAccountTarget target = context.get("account", StructureAccountTarget.class);
        AccountEntity account = target.account();
        context.reply("%s «%s»: %s %s.",
                target.type().displayName(), account.getOwnerName(), account.getBalance(), coins(account.getBalance()));
    }

    public void handleDeposit(CommandContext context) {
        StructureAccountTarget target = context.get("account", StructureAccountTarget.class);
        int amount = context.get("amount", Integer.class);
        if (amount <= 0) {
            context.reply("Сумма должна быть больше нуля.");
            return;
        }

        var transaction = accountTransactionService.deposit(
                target.account().getId(),
                amount,
                UUID.randomUUID(),
                context.getOrDefault("description", String.class, "Пополнение казны")
        );
        context.reply("Внесено %s %s на счёт %s «%s». Баланс: %s %s.",
                transaction.getAmount(), coins(transaction.getAmount()), target.type().displayName(), target.name(),
                transaction.getToBalanceAfter(), coins(transaction.getToBalanceAfter()));
    }

    public void handleWithdraw(CommandContext context) {
        StructureAccountTarget target = context.get("account", StructureAccountTarget.class);
        int amount = context.get("amount", Integer.class);
        if (amount <= 0) {
            context.reply("Сумма должна быть больше нуля.");
            return;
        }

        try {
            var transaction = accountTransactionService.withdraw(
                    target.account().getId(),
                    amount,
                    UUID.randomUUID(),
                    context.getOrDefault("description", String.class, "Снятие из казны")
            );
            context.reply("Снято %s %s со счёта %s «%s». Остаток: %s %s.",
                    transaction.getAmount(), coins(transaction.getAmount()), target.type().displayName(), target.name(),
                    transaction.getFromBalanceAfter(), coins(transaction.getFromBalanceAfter()));
        } catch (InsufficientFundsException e) {
            long balance = target.account().getBalance();
            context.reply("Недостаточно средств: на счёте %s «%s» %s %s.",
                    target.type().displayName(), target.name(), balance, coins(balance));
        }
    }

    public void handleList(CommandContext context) {
        StructureAccountType type = context.get("type", StructureAccountType.class);
        List<AccountEntity> accounts = accountService.getAccountsByOwnerTypeAndCode(type.ownerType(), type.accountCode());

        if (accounts.isEmpty()) {
            context.reply("Нет счетов типа «%s».", type.displayName());
            return;
        }

        context.reply("Счета — %s:", type.displayName());
        for (AccountEntity account : accounts) {
            context.reply("• %s — %s %s", account.getOwnerName(), account.getBalance(), coins(account.getBalance()));
        }
    }

    private static String coins(long amount) {
        return RussianLiteralsUtils.declineWord((int) amount, "монета", "монеты", "монет");
    }
}
