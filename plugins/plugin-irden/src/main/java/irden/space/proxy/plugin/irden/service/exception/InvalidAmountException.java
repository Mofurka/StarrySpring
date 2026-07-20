package irden.space.proxy.plugin.irden.service.exception;

public class InvalidAmountException extends BankingException {

    public InvalidAmountException(long amount) {
        super(
                "Amount must be greater than zero: " + amount
        );
    }
}