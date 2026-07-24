package irden.space.proxy.plugin.irden.service.exception;

import java.util.UUID;

public class AccountNotFoundException extends BankingException {

    public AccountNotFoundException(UUID accountId) {
        super("Account not found: " + accountId);
    }
    public AccountNotFoundException(String message) {
        super(message);
    }
}