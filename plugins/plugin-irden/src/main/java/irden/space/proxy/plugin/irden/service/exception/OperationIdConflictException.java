package irden.space.proxy.plugin.irden.service.exception;

import java.util.UUID;

public class OperationIdConflictException extends BankingException {

    public OperationIdConflictException(UUID operationId) {
        super(
                "Operation ID is already used by another command: "
                + operationId
        );
    }

    public OperationIdConflictException(
            UUID operationId,
            Throwable cause
    ) {
        super(
                "Operation ID conflict: " + operationId,
                cause
        );
    }
}