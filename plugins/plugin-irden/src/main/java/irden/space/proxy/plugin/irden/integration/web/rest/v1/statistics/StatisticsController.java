package irden.space.proxy.plugin.irden.integration.web.rest.v1.statistics;

import irden.space.proxy.plugin.irden.integration.web.dto.player_app_id.PlayerAppIdParam;
import irden.space.proxy.plugin.irden.integration.web.rest.v1.constants.RestRoutes;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('SITE')")
@RequestMapping(RestRoutes.StatisticsV1.PRIVATE)
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsHandler handler;

    @GetMapping
    public ResponseEntity<StatisticsResponse> getStatistics() {
        return ResponseEntity.ok(handler.handleAllPlayers());
    }

    @GetMapping(PlayerAppIdParam.PATH)
    public ResponseEntity<StatisticsResponse> getStatisticsByPlayerAppId(
            @PathVariable(PlayerAppIdParam.NAME) long playerAppId
    ) {
        return ResponseEntity.ok(handler.handlePlayerByApplicationId(playerAppId));
    }

}
