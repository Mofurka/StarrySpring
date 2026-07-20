package irden.space.proxy.plugin.irden.service.exception;

import java.util.UUID;

public class BalanceOverflowException extends BankingException {

    public BalanceOverflowException(
            UUID accountId,
            long currentBalance,
            long amount,
            Throwable cause
    ) {
        super(
                "Balance overflow on account %s: balance=%d, amount=%d"
                        .formatted(
                                accountId,
                                currentBalance,
                                amount
                        ),
                cause
        );
    }
}