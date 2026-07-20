package irden.space.proxy.plugin.irden.persistence.repository;

import irden.space.proxy.plugin.irden.persistence.model.AccountTransactionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AccountTransactionRepository
        extends JpaRepository<AccountTransactionEntity, UUID> {

    Optional<AccountTransactionEntity> findByOperationId(
            UUID operationId
    );

    @EntityGraph(attributePaths = {
            "fromAccount",
            "toAccount"
    })
    @Query("""
            select transaction
            from AccountTransactionEntity transaction
            where transaction.fromAccount.id = :accountId
               or transaction.toAccount.id = :accountId
            order by transaction.createdAt desc
            """)
    Page<AccountTransactionEntity> findAccountHistory(
            @Param("accountId") UUID accountId,
            Pageable pageable
    );
}