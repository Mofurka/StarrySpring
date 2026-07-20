package irden.space.proxy.plugin.irden.persistence.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "account_transaction",
        schema = "irden",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_account_transaction_operation_id",
                        columnNames = "operation_id"
                )
        },
        indexes = {
                @Index(
                        name = "idx_account_transaction_from_account",
                        columnList = "from_account_id"
                ),
                @Index(
                        name = "idx_account_transaction_to_account",
                        columnList = "to_account_id"
                ),
                @Index(
                        name = "idx_account_transaction_created_at",
                        columnList = "created_at"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccountTransactionEntity extends AbstractIdEntity {


    @Column(
            name = "operation_id",
            nullable = false,
            updatable = false
    )
    private UUID operationId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "type",
            nullable = false,
            updatable = false,
            length = 32
    )
    private AccountTransactionType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "from_account_id",
            updatable = false,
            foreignKey = @ForeignKey(
                    name = "fk_account_transaction_from_account"
            )
    )
    private AccountEntity fromAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "to_account_id",
            updatable = false,
            foreignKey = @ForeignKey(
                    name = "fk_account_transaction_to_account"
            )
    )
    private AccountEntity toAccount;

    @Column(
            name = "amount",
            nullable = false,
            updatable = false
    )
    private long amount;

    /**
     * Баланс исходного счёта после операции.
     *
     * Для DEPOSIT будет null.
     */
    @Column(
            name = "from_balance_after",
            updatable = false
    )
    private Long fromBalanceAfter;

    /**
     * Баланс целевого счёта после операции.
     *
     * Для WITHDRAWAL будет null.
     */
    @Column(
            name = "to_balance_after",
            updatable = false
    )
    private Long toBalanceAfter;

    @Column(
            name = "description",
            updatable = false,
            length = 256
    )
    private String description;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    public static AccountTransactionEntity deposit(
            UUID operationId,
            AccountEntity toAccount,
            long amount,
            String description
    ) {
        AccountTransactionEntity transaction =
                new AccountTransactionEntity();

        transaction.operationId = requireOperationId(operationId);
        transaction.type = AccountTransactionType.DEPOSIT;
        transaction.toAccount = Objects.requireNonNull(
                toAccount,
                "toAccount must not be null"
        );
        transaction.amount = amount;
        transaction.toBalanceAfter = toAccount.getBalance();
        transaction.description = normalizeDescription(description);
        transaction.createdAt = Instant.now();

        return transaction;
    }

    public static AccountTransactionEntity withdrawal(
            UUID operationId,
            AccountEntity fromAccount,
            long amount,
            String description
    ) {
        AccountTransactionEntity transaction =
                new AccountTransactionEntity();

        transaction.operationId = requireOperationId(operationId);
        transaction.type = AccountTransactionType.WITHDRAWAL;
        transaction.fromAccount = Objects.requireNonNull(
                fromAccount,
                "fromAccount must not be null"
        );
        transaction.amount = amount;
        transaction.fromBalanceAfter = fromAccount.getBalance();
        transaction.description = normalizeDescription(description);
        transaction.createdAt = Instant.now();

        return transaction;
    }

    public static AccountTransactionEntity transfer(
            UUID operationId,
            AccountEntity fromAccount,
            AccountEntity toAccount,
            long amount,
            String description
    ) {
        AccountTransactionEntity transaction =
                new AccountTransactionEntity();

        transaction.operationId = requireOperationId(operationId);
        transaction.type = AccountTransactionType.TRANSFER;

        transaction.fromAccount = Objects.requireNonNull(
                fromAccount,
                "fromAccount must not be null"
        );

        transaction.toAccount = Objects.requireNonNull(
                toAccount,
                "toAccount must not be null"
        );

        transaction.amount = amount;
        transaction.fromBalanceAfter = fromAccount.getBalance();
        transaction.toBalanceAfter = toAccount.getBalance();
        transaction.description = normalizeDescription(description);
        transaction.createdAt = Instant.now();

        return transaction;
    }

    private static UUID requireOperationId(UUID operationId) {
        return Objects.requireNonNull(
                operationId,
                "operationId must not be null"
        );
    }

    private static String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }

        String normalized = description.trim();

        if (normalized.length() > 256) {
            throw new IllegalArgumentException(
                    "description must not be longer than 256 characters"
            );
        }

        return normalized;
    }

    @PrePersist
    private void validateBeforePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }

        Objects.requireNonNull(
                operationId,
                "operationId must not be null"
        );

        Objects.requireNonNull(
                type,
                "type must not be null"
        );

        if (amount <= 0) {
            throw new IllegalStateException(
                    "Transaction amount must be greater than zero"
            );
        }

        switch (type) {
            case DEPOSIT -> {
                if (fromAccount != null || toAccount == null) {
                    throw new IllegalStateException(
                            "DEPOSIT must contain only toAccount"
                    );
                }
            }

            case WITHDRAWAL -> {
                if (fromAccount == null || toAccount != null) {
                    throw new IllegalStateException(
                            "WITHDRAWAL must contain only fromAccount"
                    );
                }
            }

            case TRANSFER -> {
                if (fromAccount == null || toAccount == null) {
                    throw new IllegalStateException(
                            "TRANSFER must contain both accounts"
                    );
                }

                if (Objects.equals(
                        fromAccount.getId(),
                        toAccount.getId()
                )) {
                    throw new IllegalStateException(
                            "TRANSFER accounts must be different"
                    );
                }
            }
        }
    }
}