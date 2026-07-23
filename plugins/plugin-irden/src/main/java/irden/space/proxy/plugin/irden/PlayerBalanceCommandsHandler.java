package irden.space.proxy.plugin.irden;

import irden.space.proxy.plugin.command_handler.CommandContext;
import irden.space.proxy.plugin.command_handler.wording.RussianLiteralsUtils;
import irden.space.proxy.plugin.irden.constants.PlayerAccountDefaults;
import irden.space.proxy.plugin.irden.persistence.model.AccountEntity;
import irden.space.proxy.plugin.irden.persistence.model.AccountOwnerType;
import irden.space.proxy.plugin.irden.persistence.model.AccountTransactionEntity;
import irden.space.proxy.plugin.irden.service.AccountService;
import irden.space.proxy.plugin.irden.service.AccountTransactionService;
import irden.space.proxy.plugin.irden.service.exception.InsufficientFundsException;
import irden.space.proxy.plugin.irden.service.exception.InvalidAmountException;
import irden.space.proxy.plugin.irden.service.exception.SameAccountTransferException;
import irden.space.proxy.plugin.player_manager.command.PlayerTarget;
import irden.space.proxy.plugin.player_manager.model.Player;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlayerBalanceCommandsHandler {

    private static final int HISTORY_PAGE_SIZE = 10;
    private static final int TOP_DEFAULT_COUNT = 10;
    private static final int TOP_MAX_COUNT = 25;
    private static final DateTimeFormatter HISTORY_TIME =
            DateTimeFormatter.ofPattern("dd.MM HH:mm").withZone(ZoneId.systemDefault());

    private final AccountService accountService;
    private final AccountTransactionService accountTransactionService;

    public void handleMoneyCommand(CommandContext context) {
        context.sender(Player.class).ifPresent(player -> {
            UUID accountId = requireAccount(context, player);
            if (accountId == null) {
                return;
            }
            AccountEntity account = accountService.getAccount(accountId);
            long balance = account.getBalance();
            context.reply("У вас %s %s", balance, getDeclinedAmount(balance));
        });
    }

    public void handleMoneyGiveCommand(CommandContext context) {
        context.sender(Player.class).ifPresent(sender -> {
            UUID senderAccountId = requireAccount(context, sender);
            if (senderAccountId == null) {
                return;
            }

            Player recipient = context.get("player", PlayerTarget.class).player();
            UUID recipientAccountId = (UUID) recipient.metadata().get(PlayerAccountDefaults.PLAYER_METADATA_ACCOUNT_KEY);
            if (recipientAccountId == null) {
                context.reply("Счёт игрока %s ещё не готов. Попробуйте чуть позже.", recipient.name());
                return;
            }

            int amount = context.get("amount", Integer.class);
            if (amount <= 0) {
                context.reply("Сумма должна быть больше нуля.");
                return;
            }

            try {
                var transaction = accountTransactionService.transfer(
                        senderAccountId,
                        recipientAccountId,
                        amount,
                        UUID.randomUUID(),
                        context.getOrDefault("description", String.class, "")
                );
                context.reply("Передано %s %s игроку %s", transaction.getAmount(), getDeclinedAmount(transaction.getAmount()), recipient.name());
                recipient.sendMessage("%s передал вам %s %s", sender.nickname(), transaction.getAmount(), getDeclinedAmount(transaction.getAmount()));
            } catch (InsufficientFundsException e) {
                replyInsufficientFunds(context, senderAccountId, amount);
            } catch (SameAccountTransferException e) {
                context.reply("Нельзя перевести деньги самому себе.");
            } catch (InvalidAmountException e) {
                context.reply("Сумма должна быть больше нуля.");
            }
        });
    }

    public void handleMoneyDropCommand(CommandContext context) {
        context.sender(Player.class).ifPresent(sender -> {
            UUID senderAccountId = requireAccount(context, sender);
            if (senderAccountId == null) {
                return;
            }

            int amount = context.get("amount", Integer.class);
            if (amount <= 0) {
                context.reply("Сумма должна быть больше нуля.");
                return;
            }

            try {
                var transaction = accountTransactionService.withdraw(
                        senderAccountId,
                        amount,
                        UUID.randomUUID(),
                        context.getOrDefault("description", String.class, "")
                );
                context.reply("Выброшено %s %s", transaction.getAmount(), getDeclinedAmount(transaction.getAmount()));
            } catch (InsufficientFundsException e) {
                replyInsufficientFunds(context, senderAccountId, amount);
            } catch (InvalidAmountException e) {
                context.reply("Сумма должна быть больше нуля.");
            }
        });
    }

    public void handleMoneyHistoryCommand(CommandContext context) {
        context.sender(Player.class).ifPresent(player -> {
            UUID accountId = requireAccount(context, player);
            if (accountId == null) {
                return;
            }

            int requestedPage = Math.max(1, context.getOrDefault("page", Integer.class, 1));
            Page<AccountTransactionEntity> history = accountTransactionService.getAccountHistory(
                    accountId,
                    PageRequest.of(requestedPage - 1, HISTORY_PAGE_SIZE)
            );

            if (history.isEmpty()) {
                context.reply(requestedPage == 1
                        ? "У вас пока нет операций."
                        : "На этой странице операций нет.");
                return;
            }

            context.reply("История операций (страница %d из %d):", requestedPage, history.getTotalPages());
            for (AccountTransactionEntity transaction : history.getContent()) {
                context.reply(formatHistoryLine(accountId, transaction));
            }
        });
    }

    public void handleMoneyTopCommand(CommandContext context) {
        int count = Math.clamp(context.getOrDefault("count", Integer.class, TOP_DEFAULT_COUNT), 1, TOP_MAX_COUNT);
        List<AccountEntity> top = accountService.getTopAccountsByBalance(AccountOwnerType.CHARACTER, count);

        if (top.isEmpty()) {
            context.reply("Пока нет ни одного счёта.");
            return;
        }

        context.reply("Самые богатые игроки:");
        int rank = 1;
        for (AccountEntity account : top) {
            long balance = account.getBalance();
            context.reply("%d. %s — %s %s", rank++, account.getOwnerName(), balance, getDeclinedAmount(balance));
        }
    }


    public void handleEcoGiveCommand(CommandContext context) {
        Player target = context.get("player", PlayerTarget.class).player();
        int amount = context.get("amount", Integer.class);
        if (amount <= 0) {
            context.reply("Сумма должна быть больше нуля.");
            return;
        }

        AccountEntity account = findMainAccount(context, target);
        if (account == null) {
            return;
        }

        var transaction = accountTransactionService.deposit(
                account.getId(),
                amount,
                UUID.randomUUID(),
                context.getOrDefault("description", String.class, "Начисление администратором")
        );

        context.reply("Игроку %s начислено %s %s.", target.name(), transaction.getAmount(), getDeclinedAmount(transaction.getAmount()));
        target.sendMessage("Вам начислено %s %s.", transaction.getAmount(), getDeclinedAmount(transaction.getAmount()));
    }


    public void handleEcoTakeCommand(CommandContext context) {
        Player target = context.get("player", PlayerTarget.class).player();
        int amount = context.get("amount", Integer.class);
        if (amount <= 0) {
            context.reply("Сумма должна быть больше нуля.");
            return;
        }

        AccountEntity account = findMainAccount(context, target);
        if (account == null) {
            return;
        }

        try {
            var transaction = accountTransactionService.withdraw(
                    account.getId(),
                    amount,
                    UUID.randomUUID(),
                    context.getOrDefault("description", String.class, "Списание администратором")
            );
            context.reply("У игрока %s списано %s %s.", target.name(), transaction.getAmount(), getDeclinedAmount(transaction.getAmount()));
            target.sendMessage("С вашего счёта списано %s %s.", transaction.getAmount(), getDeclinedAmount(transaction.getAmount()));
        } catch (InsufficientFundsException e) {
            long balance = account.getBalance();
            context.reply("У игрока %s недостаточно средств: на счёте %s %s.", target.name(), balance, getDeclinedAmount(balance));
        }
    }


    public void handleEcoTransferCommand(CommandContext context) {
        Player from = context.get("from", PlayerTarget.class).player();
        Player to = context.get("to", PlayerTarget.class).player();
        int amount = context.get("amount", Integer.class);
        if (amount <= 0) {
            context.reply("Сумма должна быть больше нуля.");
            return;
        }

        AccountEntity fromAccount = findMainAccount(context, from);
        if (fromAccount == null) {
            return;
        }
        AccountEntity toAccount = findMainAccount(context, to);
        if (toAccount == null) {
            return;
        }

        try {
            var transaction = accountTransactionService.transfer(
                    fromAccount.getId(),
                    toAccount.getId(),
                    amount,
                    UUID.randomUUID(),
                    context.getOrDefault("description", String.class, "Перевод администратором")
            );
            String coins = getDeclinedAmount(transaction.getAmount());
            context.reply("Переведено %s %s: %s → %s.", transaction.getAmount(), coins, from.name(), to.name());
            from.sendMessage("С вашего счёта переведено %s %s игроку %s.", transaction.getAmount(), coins, to.name());
            to.sendMessage("Вам переведено %s %s от %s.", transaction.getAmount(), coins, from.name());
        } catch (InsufficientFundsException e) {
            long balance = fromAccount.getBalance();
            context.reply("У игрока %s недостаточно средств: на счёте %s %s.", from.name(), balance, getDeclinedAmount(balance));
        } catch (SameAccountTransferException e) {
            context.reply("Нельзя перевести деньги на тот же счёт.");
        }
    }


    private AccountEntity findMainAccount(CommandContext context, Player target) {
        try {
            return accountService.getAccount(
                    AccountOwnerType.CHARACTER,
                    target.uuid().toString(),
                    PlayerAccountDefaults.PLAYER_DEFAULT_ACCOUNT_CODE
            );
        } catch (IllegalArgumentException e) {
            context.reply("У игрока %s ещё нет счёта.", target.name());
            return null;
        }
    }


    private UUID requireAccount(CommandContext context, Player player) {
        Object accountId = player.metadata().get(PlayerAccountDefaults.PLAYER_METADATA_ACCOUNT_KEY);
        if (accountId == null) {
            context.reply("Ваш счёт ещё не готов. Попробуйте через пару секунд.");
            return null;
        }
        return (UUID) accountId;
    }

    private void replyInsufficientFunds(CommandContext context, UUID accountId, long requested) {
        long balance = accountService.getAccount(accountId).getBalance();
        context.reply("Недостаточно средств: у вас %s %s, а нужно %s.", balance, getDeclinedAmount(balance), requested);
    }

    private String formatHistoryLine(UUID accountId, AccountTransactionEntity transaction) {
        long amount = transaction.getAmount();
        String coins = getDeclinedAmount(amount);
        String when = HISTORY_TIME.format(transaction.getCreatedAt());

        return switch (transaction.getType()) {
            case DEPOSIT -> "[%s] +%d %s (начисление)".formatted(when, amount, coins);
            case WITHDRAWAL -> "[%s] -%d %s (списание)".formatted(when, amount, coins);
            case TRANSFER -> {
                boolean outgoing = transaction.getFromAccount() != null
                        && accountId.equals(transaction.getFromAccount().getId());
                if (outgoing) {
                    yield "[%s] -%d %s → %s".formatted(when, amount, coins, counterpartyName(transaction.getToAccount()));
                }
                yield "[%s] +%d %s ← %s".formatted(when, amount, coins, counterpartyName(transaction.getFromAccount()));
            }
        };
    }

    private String counterpartyName(AccountEntity account) {
        return account != null ? account.getOwnerName() : "?";
    }


    private String getDeclinedAmount(long amount) {
        return RussianLiteralsUtils.declineWord((int) amount, "монета", "монеты", "монет");
    }
}
