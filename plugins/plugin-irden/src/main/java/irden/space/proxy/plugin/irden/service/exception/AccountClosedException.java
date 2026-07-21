package irden.space.proxy.plugin.irden.service.exception;

import java.util.UUID;

public class AccountClosedException extends BankingException {

    public AccountClosedException(UUID accountId) {
        super("Account is closed: " + accountId);
    }
}