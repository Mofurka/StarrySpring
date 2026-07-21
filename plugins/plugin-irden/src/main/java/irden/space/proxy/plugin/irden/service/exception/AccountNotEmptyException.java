package irden.space.proxy.plugin.irden.service.exception;

import java.util.UUID;

public class AccountNotEmptyException extends BankingException {

    public AccountNotEmptyException(
            UUID accountId,
            long balance
    ) {
        super(
                "Cannot close account %s with non-zero balance: %d"
                        .formatted(accountId, balance)
        );
    }
}