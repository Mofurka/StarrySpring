package irden.space.proxy.plugin.irden;

import irden.space.proxy.plugin.command_handler.CommandContext;
import irden.space.proxy.plugin.command_handler.wording.RussianLiteralsUtils;
import irden.space.proxy.plugin.irden.account.StructureAccountTarget;
import irden.space.proxy.plugin.irden.account.StructureAccountType;
import irden.space.proxy.plugin.irden.persistence.model.account.AccountEntity;
import irden.space.proxy.plugin.irden.service.AccountService;
import irden.space.proxy.plugin.irden.service.AccountTransactionService;
import irden.space.proxy.plugin.irden.service.PlayerAccountService;
import irden.space.proxy.plugin.irden.service.exception.AccountAlreadyExistsException;
import irden.space.proxy.plugin.irden.service.exception.AccountNotFoundException;
import irden.space.proxy.plugin.irden.service.exception.InsufficientFundsException;
import irden.space.proxy.plugin.irden.service.exception.SameAccountTransferException;
import irden.space.proxy.plugin.player_manager.command.PlayerTarget;
import irden.space.proxy.plugin.player_manager.model.Player;
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
    private final PlayerAccountService playerAccountService;
    private final AccountTransactionService accountTransactionService;

    private static String coins(long amount) {
        return RussianLiteralsUtils.declineWord((int) amount, "монета", "монеты", "монет");
    }

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
            context.reply("Создан счёт: %s '%s' (баланс %s %s). UUID: %s",
                    type.displayName(), account.getOwnerName(), account.getBalance(), coins(account.getBalance()), account.getId());
        } catch (AccountAlreadyExistsException e) {
            context.reply("Счёт %s '%s' уже существует.", type.displayName(), name);
        } catch (IllegalArgumentException e) {
            context.reply("Не удалось создать счёт: %s", e.getMessage());
        }
    }

    public void handleInfo(CommandContext context) {
        StructureAccountTarget target = context.get("account", StructureAccountTarget.class);
        AccountEntity account = target.account();
        context.reply("%s '%s': %s %s. UUID: %s",
                target.type().displayName(), account.getOwnerName(), account.getBalance(), coins(account.getBalance()), account.getId());
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
        context.reply("Внесено %s %s на счёт %s '%s'. Баланс: %s %s.",
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
            context.reply("Снято %s %s со счёта %s '%s'. Остаток: %s %s.",
                    transaction.getAmount(), coins(transaction.getAmount()), target.type().displayName(), target.name(),
                    transaction.getFromBalanceAfter(), coins(transaction.getFromBalanceAfter()));
        } catch (InsufficientFundsException e) {
            long balance = target.account().getBalance();
            context.reply("Недостаточно средств: на счёте %s '%s' %s %s.",
                    target.type().displayName(), target.name(), balance, coins(balance));
        }
    }

    public void handlePay(CommandContext context) {
        transferWithPlayer(context, Direction.TO_PLAYER);
    }

    public void handleCollect(CommandContext context) {
        transferWithPlayer(context, Direction.FROM_PLAYER);
    }

    private void transferWithPlayer(CommandContext context, Direction direction) {
        StructureAccountTarget target = context.get("account", StructureAccountTarget.class);
        Player player = context.get("player", PlayerTarget.class).player();

        int amount = context.get("amount", Integer.class);
        if (amount <= 0) {
            context.reply("Сумма должна быть больше нуля.");
            return;
        }

        AccountEntity playerAccount;
        try {
            playerAccount = playerAccountService.getMainAccount(player.uuid());
        } catch (AccountNotFoundException | IllegalArgumentException e) {
            context.reply("У игрока %s ещё нет счёта.", player.name());
            return;
        }

        AccountEntity structureAccount = target.account();
        String structure = "%s '%s'".formatted(target.type().displayName(), target.name());
        boolean toPlayer = direction == Direction.TO_PLAYER;

        try {
            var transaction = accountTransactionService.transfer(
                    toPlayer ? structureAccount.getId() : playerAccount.getId(),
                    toPlayer ? playerAccount.getId() : structureAccount.getId(),
                    amount,
                    UUID.randomUUID(),
                    context.getOrDefault("description", String.class,
                            toPlayer ? "Выплата из казны" : "Взнос в казну")
            );

            long moved = transaction.getAmount();
            if (toPlayer) {
                context.reply("Переведено %s %s: %s → %s. Остаток на счёте: %s %s.",
                        moved, coins(moved), structure, player.name(),
                        transaction.getFromBalanceAfter(), coins(transaction.getFromBalanceAfter()));
                player.sendMessage("Вам переведено %s %s со счёта %s.", moved, coins(moved), structure);
            } else {
                context.reply("Переведено %s %s: %s → %s. Баланс счёта: %s %s.",
                        moved, coins(moved), player.name(), structure,
                        transaction.getToBalanceAfter(), coins(transaction.getToBalanceAfter()));
                player.sendMessage("С вашего счёта переведено %s %s на счёт %s.", moved, coins(moved), structure);
            }
        } catch (InsufficientFundsException e) {
            if (toPlayer) {
                long balance = structureAccount.getBalance();
                context.reply("Недостаточно средств: на счёте %s %s %s.", structure, balance, coins(balance));
            } else {
                long balance = playerAccount.getBalance();
                context.reply("Недостаточно средств: у игрока %s %s %s.", player.name(), balance, coins(balance));
            }
        } catch (SameAccountTransferException e) {
            context.reply("Нельзя перевести деньги на тот же счёт.");
        }
    }

    public void handleList(CommandContext context) {
        StructureAccountType type = context.get("type", StructureAccountType.class);
        List<AccountEntity> accounts = accountService.getAccountsByOwnerTypeAndCode(type.ownerType(), type.accountCode());

        if (accounts.isEmpty()) {
            context.reply("Нет счетов типа '%s'.", type.displayName());
            return;
        }

        context.reply("Счета - %s:", type.displayName());
        for (AccountEntity account : accounts) {
            context.reply("• %s - %s %s", account.getOwnerName(), account.getBalance(), coins(account.getBalance()));
        }
    }

    private enum Direction {
        TO_PLAYER,
        FROM_PLAYER
    }
}
