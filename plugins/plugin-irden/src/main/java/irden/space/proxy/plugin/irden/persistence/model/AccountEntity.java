package irden.space.proxy.plugin.irden.persistence.model;

import irden.space.proxy.plugin.irden.service.exception.*;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

@Entity
@Table(
        name = "game_account",
        schema = "irden",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_game_account_owner_code",
                        columnNames = {
                                "owner_type",
                                "owner_id",
                                "account_code"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_game_account_owner",
                        columnList = "owner_type, owner_id"
                ),
                @Index(
                        name = "idx_game_account_status",
                        columnList = "status"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccountEntity extends AbstractIdEntity {

    private static final int MAX_OWNER_ID_LENGTH = 64;
    private static final int MAX_OWNER_NAME_LENGTH = 128;
    private static final int MAX_ACCOUNT_CODE_LENGTH = 32;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "owner_type",
            nullable = false,
            updatable = false,
            length = 32
    )
    private AccountOwnerType ownerType;

    @Column(
            name = "owner_id",
            nullable = false,
            updatable = false,
            length = MAX_OWNER_ID_LENGTH
    )
    private String ownerId;


    @Column(
            name = "owner_name",
            nullable = false,
            length = MAX_OWNER_NAME_LENGTH
    )
    private String ownerName;


    @Column(
            name = "account_code",
            nullable = false,
            updatable = false,
            length = MAX_ACCOUNT_CODE_LENGTH
    )
    private String accountCode;

    @Column(name = "balance", nullable = false)
    private long balance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private AccountStatus status;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static AccountEntity create(
            AccountOwnerType ownerType,
            String ownerId,
            String ownerName,
            String accountCode
    ) {
        AccountEntity account = new AccountEntity();

        account.ownerType = Objects.requireNonNull(
                ownerType,
                "ownerType must not be null"
        );

        account.ownerId = normalizeOwnerId(ownerId);
        account.ownerName = normalizeOwnerName(ownerName);
        account.accountCode = normalizeAccountCode(accountCode);


        account.balance = 0;
        account.status = AccountStatus.ACTIVE;

        return account;
    }

    private static void validateAmount(long amount) {
        if (amount <= 0) {
            throw new InvalidAmountException(amount);
        }
    }

    public static String normalizeOwnerId(String ownerId) {
        String normalized = requireNotBlank(
                ownerId,
                "ownerId"
        );

        if (normalized.length() > MAX_OWNER_ID_LENGTH) {
            throw new IllegalArgumentException(
                    "ownerId must not be longer than "
                    + MAX_OWNER_ID_LENGTH
            );
        }

        return normalized;
    }

    public static String normalizeOwnerName(String ownerName) {
        String normalized = requireNotBlank(
                ownerName,
                "ownerName"
        );

        if (normalized.length() > MAX_OWNER_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "ownerName must not be longer than "
                    + MAX_OWNER_NAME_LENGTH
            );
        }

        return normalized;
    }

    public static String normalizeAccountCode(String accountCode) {
        String normalized = requireNotBlank(
                accountCode,
                "accountCode"
        ).toUpperCase(Locale.ROOT);

        if (normalized.length() > MAX_ACCOUNT_CODE_LENGTH) {
            throw new IllegalArgumentException(
                    "accountCode must not be longer than "
                    + MAX_ACCOUNT_CODE_LENGTH
            );
        }

        if (!normalized.matches("[A-Z0-9_-]+")) {
            throw new IllegalArgumentException(
                    "accountCode may contain only A-Z, 0-9, _ and -"
            );
        }

        return normalized;
    }

    private static String requireNotBlank(
            String value,
            String fieldName
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank"
            );
        }

        return value.trim();
    }

    public void renameOwner(String newOwnerName) {
        this.ownerName = normalizeOwnerName(newOwnerName);
    }

    public void deposit(long amount) {
        ensureActive();
        validateAmount(amount);

        try {
            balance = Math.addExact(balance, amount);
        } catch (ArithmeticException exception) {
            throw new BalanceOverflowException(
                    getId(),
                    balance,
                    amount,
                    exception
            );
        }
    }

    public void withdraw(long amount) {
        ensureActive();
        validateAmount(amount);

        if (balance < amount) {
            throw new InsufficientFundsException(
                    getId(),
                    balance,
                    amount
            );
        }

        balance -= amount;
    }

    public void close() {
        if (status == AccountStatus.CLOSED) {
            return;
        }

        if (balance != 0) {
            throw new AccountNotEmptyException(
                    getId(),
                    balance
            );
        }

        status = AccountStatus.CLOSED;
    }

    public void ensureActive() {
        if (status != AccountStatus.ACTIVE) {
            throw new AccountClosedException(getId());
        }
    }

    @PrePersist
    private void onCreate() {
        Instant now = Instant.now();

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;

        if (status == null) {
            status = AccountStatus.ACTIVE;
        }
    }

    @PreUpdate
    private void onUpdate() {
        updatedAt = Instant.now();
    }
}