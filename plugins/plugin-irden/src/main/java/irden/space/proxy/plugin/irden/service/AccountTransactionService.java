package irden.space.proxy.plugin.irden.service;

import irden.space.proxy.plugin.irden.persistence.model.AccountEntity;
import irden.space.proxy.plugin.irden.persistence.model.AccountTransactionEntity;
import irden.space.proxy.plugin.irden.persistence.model.AccountTransactionType;
import irden.space.proxy.plugin.irden.persistence.repository.AccountRepository;
import irden.space.proxy.plugin.irden.persistence.repository.AccountTransactionRepository;
import irden.space.proxy.plugin.irden.service.exception.AccountNotFoundException;
import irden.space.proxy.plugin.irden.service.exception.InvalidAmountException;
import irden.space.proxy.plugin.irden.service.exception.OperationIdConflictException;
import irden.space.proxy.plugin.irden.service.exception.SameAccountTransferException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountTransactionService {

    private final AccountRepository accountRepository;
    private final AccountTransactionRepository transactionRepository;

    @Transactional
    public AccountTransactionEntity deposit(
            UUID accountId,
            long amount,
            UUID operationId,
            String description
    ) {
        validateOperationId(operationId);
        validateAmount(amount);

        AccountTransactionEntity existing =
                findExistingOperation(
                        operationId,
                        AccountTransactionType.DEPOSIT,
                        null,
                        accountId,
                        amount
                );

        if (existing != null) {
            return existing;
        }

        AccountEntity account = lockAccount(accountId);

        existing = findExistingOperation(
                operationId,
                AccountTransactionType.DEPOSIT,
                null,
                accountId,
                amount
        );

        if (existing != null) {
            return existing;
        }

        account.deposit(amount);

        AccountTransactionEntity transaction =
                AccountTransactionEntity.deposit(
                        operationId,
                        account,
                        amount,
                        description
                );

        return saveTransaction(transaction);
    }

    @Transactional
    public AccountTransactionEntity withdraw(
            UUID accountId,
            long amount,
            UUID operationId,
            String description
    ) {
        validateOperationId(operationId);
        validateAmount(amount);

        AccountTransactionEntity existing =
                findExistingOperation(
                        operationId,
                        AccountTransactionType.WITHDRAWAL,
                        accountId,
                        null,
                        amount
                );

        if (existing != null) {
            return existing;
        }

        AccountEntity account = lockAccount(accountId);

        existing = findExistingOperation(
                operationId,
                AccountTransactionType.WITHDRAWAL,
                accountId,
                null,
                amount
        );

        if (existing != null) {
            return existing;
        }

        account.withdraw(amount);

        AccountTransactionEntity transaction =
                AccountTransactionEntity.withdrawal(
                        operationId,
                        account,
                        amount,
                        description
                );

        return saveTransaction(transaction);
    }

    @Transactional
    public AccountTransactionEntity transfer(
            UUID fromAccountId,
            UUID toAccountId,
            long amount,
            UUID operationId,
            String description
    ) {
        Objects.requireNonNull(
                fromAccountId,
                "fromAccountId must not be null"
        );

        Objects.requireNonNull(
                toAccountId,
                "toAccountId must not be null"
        );

        validateOperationId(operationId);
        validateAmount(amount);

        if (fromAccountId.equals(toAccountId)) {
            throw new SameAccountTransferException(
                    fromAccountId
            );
        }

        AccountTransactionEntity existing =
                findExistingOperation(
                        operationId,
                        AccountTransactionType.TRANSFER,
                        fromAccountId,
                        toAccountId,
                        amount
                );

        if (existing != null) {
            return existing;
        }

        LockedAccounts accounts = lockAccounts(
                fromAccountId,
                toAccountId
        );

        existing = findExistingOperation(
                operationId,
                AccountTransactionType.TRANSFER,
                fromAccountId,
                toAccountId,
                amount
        );

        if (existing != null) {
            return existing;
        }

        accounts.source().withdraw(amount);
        accounts.target().deposit(amount);

        AccountTransactionEntity transaction =
                AccountTransactionEntity.transfer(
                        operationId,
                        accounts.source(),
                        accounts.target(),
                        amount,
                        description
                );

        return saveTransaction(transaction);
    }

    @Transactional(readOnly = true)
    public Page<AccountTransactionEntity> getAccountHistory(
            UUID accountId,
            Pageable pageable
    ) {
        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException(accountId);
        }

        return transactionRepository.findAccountHistory(
                accountId,
                pageable
        );
    }

    private AccountTransactionEntity findExistingOperation(
            UUID operationId,
            AccountTransactionType expectedType,
            UUID expectedFromAccountId,
            UUID expectedToAccountId,
            long expectedAmount
    ) {
        return transactionRepository
                .findByOperationId(operationId)
                .map(transaction -> {
                    validateExistingOperation(
                            transaction,
                            expectedType,
                            expectedFromAccountId,
                            expectedToAccountId,
                            expectedAmount
                    );

                    return transaction;
                })
                .orElse(null);
    }

    private void validateExistingOperation(
            AccountTransactionEntity transaction,
            AccountTransactionType expectedType,
            UUID expectedFromAccountId,
            UUID expectedToAccountId,
            long expectedAmount
    ) {
        UUID actualFromAccountId =
                getAccountId(transaction.getFromAccount());

        UUID actualToAccountId =
                getAccountId(transaction.getToAccount());

        boolean sameOperation =
                transaction.getType() == expectedType
                && transaction.getAmount() == expectedAmount
                && Objects.equals(
                        actualFromAccountId,
                        expectedFromAccountId
                )
                && Objects.equals(
                        actualToAccountId,
                        expectedToAccountId
                );

        if (!sameOperation) {
            throw new OperationIdConflictException(
                    transaction.getOperationId()
            );
        }
    }

    /**
     * Блокирует два счёта всегда в одинаковом порядке.
     *
     * Это уменьшает вероятность deadlock при переводах:
     *
     * поток 1: A -> B
     * поток 2: B -> A
     */
    private LockedAccounts lockAccounts(
            UUID sourceId,
            UUID targetId
    ) {
        UUID firstId;
        UUID secondId;

        if (sourceId.compareTo(targetId) < 0) {
            firstId = sourceId;
            secondId = targetId;
        } else {
            firstId = targetId;
            secondId = sourceId;
        }

        AccountEntity first = lockAccount(firstId);
        AccountEntity second = lockAccount(secondId);

        if (first.getId().equals(sourceId)) {
            return new LockedAccounts(first, second);
        }

        return new LockedAccounts(second, first);
    }

    private AccountEntity lockAccount(UUID accountId) {
        return accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() ->
                        new AccountNotFoundException(accountId)
                );
    }

    private AccountTransactionEntity saveTransaction(
            AccountTransactionEntity transaction
    ) {
        try {
            /*
             * Принудительный flush нужен, чтобы конфликт operationId
             * произошёл до выхода из метода.
             */
            return transactionRepository.saveAndFlush(
                    transaction
            );
        } catch (DataIntegrityViolationException exception) {
            throw new OperationIdConflictException(
                    transaction.getOperationId(),
                    exception
            );
        }
    }

    private UUID getAccountId(AccountEntity account) {
        return account == null
                ? null
                : account.getId();
    }

    private void validateOperationId(UUID operationId) {
        Objects.requireNonNull(
                operationId,
                "operationId must not be null"
        );
    }

    private void validateAmount(long amount) {
        if (amount <= 0) {
            throw new InvalidAmountException(amount);
        }
    }

    private record LockedAccounts(
            AccountEntity source,
            AccountEntity target
    ) {
    }
}