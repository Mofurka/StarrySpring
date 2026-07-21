package irden.space.proxy.plugin.irden.service.exception;

import java.util.UUID;

public class InsufficientFundsException extends BankingException {

    public InsufficientFundsException(
            UUID accountId,
            long balance,
            long requestedAmount
    ) {
        super(
                "Insufficient funds on account %s: balance=%d, requested=%d"
                        .formatted(
                                accountId,
                                balance,
                                requestedAmount
                        )
        );
    }
}