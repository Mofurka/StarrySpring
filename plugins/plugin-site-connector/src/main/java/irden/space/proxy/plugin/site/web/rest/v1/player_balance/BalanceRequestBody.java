package irden.space.proxy.plugin.site.web.rest.v1.player_balance;

import com.fasterxml.jackson.annotation.JsonAlias;


public record BalanceRequestBody(
        long amount,
        @JsonAlias("from_id")
        String fromId
) {
}
