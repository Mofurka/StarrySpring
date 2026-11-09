package irden.space.proxy.plugin.irden.integration.web.rest.v1.transactions;

import com.fasterxml.jackson.annotation.JsonProperty;
import irden.space.proxy.plugin.irden.persistence.model.account.AccountEntity;
import irden.space.proxy.plugin.irden.persistence.model.account.AccountOwnerType;

import java.util.UUID;


public record TransactionAccountDto(

        @JsonProperty("account_id")
        UUID accountId,

        @JsonProperty("owner_type")
        AccountOwnerType ownerType,

        @JsonProperty("owner_id")
        String ownerId,

        @JsonProperty("owner_name")
        String ownerName,

        @JsonProperty("account_code")
        String accountCode
) {


    public static TransactionAccountDto of(AccountEntity account) {
        if (account == null) {
            return null;
        }

        return new TransactionAccountDto(
                account.getId(),
                account.getOwnerType(),
                account.getOwnerId(),
                account.getOwnerName(),
                account.getAccountCode()
        );
    }
}
