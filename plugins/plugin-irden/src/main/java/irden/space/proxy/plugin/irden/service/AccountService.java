package irden.space.proxy.plugin.irden.service;

import irden.space.proxy.plugin.irden.persistence.model.AccountEntity;
import irden.space.proxy.plugin.irden.persistence.model.AccountOwnerType;
import irden.space.proxy.plugin.irden.persistence.model.AccountStatus;
import irden.space.proxy.plugin.irden.persistence.repository.AccountRepository;
import irden.space.proxy.plugin.irden.service.exception.AccountAlreadyExistsException;
import irden.space.proxy.plugin.irden.service.exception.AccountNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    @Transactional
    public AccountEntity createAccount(
            AccountOwnerType ownerType,
            String ownerId,
            String ownerName,
            String accountCode
    ) {
        String normalizedOwnerId =
                AccountEntity.normalizeOwnerId(ownerId);

        String normalizedAccountCode =
                AccountEntity.normalizeAccountCode(accountCode);

        AccountEntity account = AccountEntity.create(
                ownerType,
                normalizedOwnerId,
                ownerName,
                normalizedAccountCode
        );

        try {
            /*
             * saveAndFlush нужен, чтобы уникальное ограничение
             * сработало внутри этого метода.
             */
            return accountRepository.saveAndFlush(account);
        } catch (DataIntegrityViolationException exception) {
            throw new AccountAlreadyExistsException(
                    ownerType,
                    normalizedOwnerId,
                    normalizedAccountCode,
                    exception
            );
        }
    }

    @Transactional(readOnly = true)
    public AccountEntity getAccount(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() ->
                        new AccountNotFoundException(accountId)
                );
    }

    @Transactional(readOnly = true)
    public AccountEntity getAccount(
            AccountOwnerType ownerType,
            String ownerId,
            String accountCode
    ) throws IllegalArgumentException {
        String normalizedOwnerId =
                AccountEntity.normalizeOwnerId(ownerId);

        String normalizedAccountCode =
                AccountEntity.normalizeAccountCode(accountCode);

        return accountRepository
                .findByOwnerTypeAndOwnerIdAndAccountCode(
                        ownerType,
                        normalizedOwnerId,
                        normalizedAccountCode
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "Account not found: ownerType=%s, ownerId=%s, accountCode=%s"
                                .formatted(
                                        ownerType,
                                        normalizedOwnerId,
                                        normalizedAccountCode
                                )
                ));
    }


    @Transactional(readOnly = true)
    public List<AccountEntity> getTopAccountsByBalance(
            AccountOwnerType ownerType,
            int limit
    ) {
        return accountRepository.findByOwnerTypeAndStatusOrderByBalanceDesc(
                ownerType,
                AccountStatus.ACTIVE,
                PageRequest.of(0, limit)
        );
    }


    @Transactional(readOnly = true)
    public List<AccountEntity> getAccountsByOwnerTypeAndCode(
            AccountOwnerType ownerType,
            String accountCode
    ) {
        return accountRepository.findByOwnerTypeAndAccountCodeOrderByOwnerName(
                ownerType,
                AccountEntity.normalizeAccountCode(accountCode)
        );
    }

    @Transactional(readOnly = true)
    public List<AccountEntity> getOwnerAccounts(
            AccountOwnerType ownerType,
            String ownerId
    ) {
        return accountRepository
                .findAllByOwnerTypeAndOwnerIdOrderByAccountCode(
                        ownerType,
                        AccountEntity.normalizeOwnerId(ownerId)
                );
    }

    @Transactional
    public AccountEntity renameOwner(
            UUID accountId,
            String newOwnerName
    ) {
        AccountEntity account = lockAccount(accountId);
        account.renameOwner(newOwnerName);

        return account;
    }

    @Transactional
    public AccountEntity closeAccount(UUID accountId) {
        AccountEntity account = lockAccount(accountId);
        account.close();

        return account;
    }

    private AccountEntity lockAccount(UUID accountId) {
        return accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() ->
                        new AccountNotFoundException(accountId)
                );
    }
}