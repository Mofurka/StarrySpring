package irden.space.proxy.plugin.irden.service.exception;

import java.util.UUID;

public class SameAccountTransferException extends BankingException {

    public SameAccountTransferException(UUID accountId) {
        super(
                "Cannot transfer money to the same account: "
                + accountId
        );
    }
}