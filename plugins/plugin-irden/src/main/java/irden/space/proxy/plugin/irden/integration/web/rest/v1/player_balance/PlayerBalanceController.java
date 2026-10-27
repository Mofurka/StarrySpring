package irden.space.proxy.plugin.irden.integration.web.rest.v1.player_balance;

import irden.space.proxy.plugin.irden.integration.web.dto.player_app_id.PlayerAppIdParam;
import irden.space.proxy.plugin.irden.integration.web.rest.v1.constants.RestRoutes;
import irden.space.proxy.plugin.irden.integration.web.rest.v1.dto.player_uuid.PlayerUuidParam;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(RestRoutes.MoneyV1.PRIVATE)
@PreAuthorize("hasRole('SITE')")
@RequiredArgsConstructor
public class PlayerBalanceController {
    private final PlayerBalanceHandler handler;


    @GetMapping(PlayerAppIdParam.PATH)
    public long getPlayerBalance(
            @PathVariable(PlayerAppIdParam.NAME) PlayerAppIdParam playerAppIdParam
    ) {
        return handler.handleGetPlayerBalance(playerAppIdParam);
    }

    @DeleteMapping(PlayerAppIdParam.PATH)
    public long resetPlayerBalance(
            @PathVariable(PlayerAppIdParam.NAME) PlayerAppIdParam playerAppIdParam
    ) {
        return handler.handleResetPlayerBalance(playerAppIdParam);
    }

    @PutMapping(PlayerAppIdParam.PATH)
    public long setPlayerBalance(
            @PathVariable(PlayerAppIdParam.NAME) PlayerAppIdParam playerAppIdParam,
            @RequestBody BalanceRequestBody balanceRequestBody
    ) {
        return handler.setPlayerBalance(playerAppIdParam, balanceRequestBody);
    }

    @PatchMapping(PlayerAppIdParam.PATH)
    public long updatePlayerBalance(
            @PathVariable(PlayerAppIdParam.NAME) PlayerAppIdParam playerAppIdParam,
            @RequestBody BalanceRequestBody balanceRequestBody
    ) {
        return handler.updatePlayerBalance(playerAppIdParam, balanceRequestBody);
    }
}
