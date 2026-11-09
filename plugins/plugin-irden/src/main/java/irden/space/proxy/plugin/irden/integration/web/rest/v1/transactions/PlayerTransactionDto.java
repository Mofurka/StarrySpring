package irden.space.proxy.plugin.irden.integration.web.rest.v1.transactions;

import com.fasterxml.jackson.annotation.JsonProperty;
import irden.space.proxy.plugin.irden.persistence.model.account.AccountTransactionType;

import java.time.LocalDateTime;
import java.util.UUID;


public record PlayerTransactionDto(

        @JsonProperty("operation_id")
        UUID operationId,

        AccountTransactionType type,

        TransactionDirection direction,

        long amount,

        @JsonProperty("balance_after")
        Long balanceAfter,

        String description,

        @JsonProperty("created_at")
        LocalDateTime createdAt,

        TransactionAccountDto from,

        TransactionAccountDto to
) {
}
