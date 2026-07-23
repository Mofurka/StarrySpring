package irden.space.proxy.plugin.irden.persistence.repository;

import irden.space.proxy.plugin.irden.persistence.model.AccountEntity;
import irden.space.proxy.plugin.irden.persistence.model.AccountOwnerType;
import irden.space.proxy.plugin.irden.persistence.model.AccountStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository
        extends JpaRepository<AccountEntity, UUID> {

    Optional<AccountEntity>
    findByOwnerTypeAndOwnerIdAndAccountCode(
            AccountOwnerType ownerType,
            String ownerId,
            String accountCode
    );


    List<AccountEntity> findByOwnerTypeAndStatusOrderByBalanceDesc(
            AccountOwnerType ownerType,
            AccountStatus status,
            Pageable pageable
    );


    List<AccountEntity> findByOwnerTypeAndAccountCodeOrderByOwnerName(
            AccountOwnerType ownerType,
            String accountCode
    );

    List<AccountEntity>
    findAllByOwnerTypeAndOwnerIdOrderByAccountCode(
            AccountOwnerType ownerType,
            String ownerId
    );

    boolean existsByOwnerTypeAndOwnerIdAndAccountCode(
            AccountOwnerType ownerType,
            String ownerId,
            String accountCode
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select account
            from AccountEntity account
            where account.id = :accountId
            """)
    Optional<AccountEntity> findByIdForUpdate(
            @Param("accountId") UUID accountId
    );
}