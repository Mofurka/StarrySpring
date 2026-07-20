package irden.space.proxy.plugin.irden.service.exception;

import irden.space.proxy.plugin.irden.persistence.model.AccountOwnerType;

public class AccountAlreadyExistsException extends BankingException {

    public AccountAlreadyExistsException(
            AccountOwnerType ownerType,
            String ownerId,
            String accountCode,
            Throwable cause
    ) {
        super(
                "Account already exists: ownerType=%s, ownerId=%s, accountCode=%s"
                        .formatted(
                                ownerType,
                                ownerId,
                                accountCode
                        ),
                cause
        );
    }
}