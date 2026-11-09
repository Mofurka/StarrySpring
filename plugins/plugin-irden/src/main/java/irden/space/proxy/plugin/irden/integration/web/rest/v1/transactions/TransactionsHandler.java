package irden.space.proxy.plugin.irden.integration.web.rest.v1.transactions;

import irden.space.proxy.plugin.irden.constants.PlayerAccountDefaults;
import irden.space.proxy.plugin.irden.persistence.model.account.AccountEntity;
import irden.space.proxy.plugin.irden.persistence.model.account.AccountOwnerType;
import irden.space.proxy.plugin.irden.persistence.model.account.AccountTransactionEntity;
import irden.space.proxy.plugin.irden.service.AccountService;
import irden.space.proxy.plugin.irden.service.AccountTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionsHandler {

    private final AccountService accountService;
    private final AccountTransactionService accountTransactionService;

    public PagedModel<PlayerTransactionDto> handlePlayerTransactions(
            long applicationId,
            TransactionsDateRange range,
            Pageable pageable
    ) {
        List<AccountEntity> accounts = accountService.getOwnerAccounts(
                AccountOwnerType.CHARACTER,
                PlayerAccountDefaults.playerOwnerId(applicationId)
        );

        if (accounts.isEmpty()) {
            return new PagedModel<>(Page.empty(pageable));
        }

        Set<UUID> accountIds = accounts.stream()
                .map(AccountEntity::getId)
                .collect(Collectors.toSet());

        Page<AccountTransactionEntity> history = accountTransactionService.getAccountsHistory(
                accountIds,
                range.from(),
                range.toExclusive(),
                pageable
        );

        return new PagedModel<>(history.map(transaction -> toDto(transaction, accountIds)));
    }


    private PlayerTransactionDto toDto(AccountTransactionEntity transaction, Set<UUID> playerAccountIds) {
        AccountEntity from = transaction.getFromAccount();
        boolean outgoing = from != null && playerAccountIds.contains(from.getId());

        return new PlayerTransactionDto(
                transaction.getOperationId(),
                transaction.getType(),
                outgoing ? TransactionDirection.OUT : TransactionDirection.IN,
                transaction.getAmount(),
                outgoing ? transaction.getFromBalanceAfter() : transaction.getToBalanceAfter(),
                transaction.getDescription(),
                LocalDateTime.ofInstant(transaction.getCreatedAt(), ZoneId.systemDefault()),
                TransactionAccountDto.of(from),
                TransactionAccountDto.of(transaction.getToAccount())
        );
    }
}
