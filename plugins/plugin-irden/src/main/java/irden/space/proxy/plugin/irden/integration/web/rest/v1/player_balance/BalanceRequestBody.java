package irden.space.proxy.plugin.irden.integration.web.rest.v1.player_balance;


public record BalanceRequestBody(
        long amount,
        String comment) {
}
