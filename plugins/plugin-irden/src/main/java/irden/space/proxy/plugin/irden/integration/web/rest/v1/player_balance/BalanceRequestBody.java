package irden.space.proxy.plugin.irden.integration.web.rest.v1.player_balance;

import com.fasterxml.jackson.annotation.JsonProperty;


public record BalanceRequestBody(
        long amount,
        String comment,
        @JsonProperty("from_app_id")
        String fromAppId
) {
}
