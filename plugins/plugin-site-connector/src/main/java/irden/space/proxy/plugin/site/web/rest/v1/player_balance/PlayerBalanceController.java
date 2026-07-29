package irden.space.proxy.plugin.site.web.rest.v1.player_balance;

import irden.space.proxy.plugin.site.web.rest.v1.constants.RestRoutes;
import irden.space.proxy.plugin.site.web.rest.v1.dto.player_uuid.PlayerUuidParam;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(RestRoutes.MoneyV1.PRIVATE)
@PreAuthorize("hasRole('SITE')")
@RequiredArgsConstructor
public class PlayerBalanceController {
    private final PlayerBalanceHandler handler;


    @GetMapping(PlayerUuidParam.PATH)
    public long getPlayerBalance(
            @PathVariable(PlayerUuidParam.NAME) PlayerUuidParam playerUuidParam
    ) {
        return handler.handleGetPlayerBalance(playerUuidParam);
    }

    @DeleteMapping(PlayerUuidParam.PATH)
    public long resetPlayerBalance(
            @PathVariable(PlayerUuidParam.NAME) PlayerUuidParam playerUuidParam
    ) {
        return handler.handleResetPlayerBalance(playerUuidParam);
    }

    @PutMapping(PlayerUuidParam.PATH)
    public long setPlayerBalance(
            @PathVariable(PlayerUuidParam.NAME) PlayerUuidParam playerUuidParam,
            @RequestBody BalanceRequestBody balanceRequestBody
    ) {
        return handler.setPlayerBalance(playerUuidParam, balanceRequestBody);
    }

    @PatchMapping(PlayerUuidParam.PATH)
    public long updatePlayerBalance(
            @PathVariable(PlayerUuidParam.NAME) PlayerUuidParam playerUuidParam,
            @RequestBody BalanceRequestBody balanceRequestBody
    ) {
        return handler.updatePlayerBalance(playerUuidParam, balanceRequestBody);
    }
}
