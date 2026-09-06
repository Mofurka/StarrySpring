package irden.space.proxy.plugin.irden.integration.web.rest.v1.statistics;

import irden.space.proxy.plugin.irden.integration.web.dto.player_app_id.PlayerAppIdParam;
import irden.space.proxy.plugin.irden.integration.web.rest.v1.constants.RestRoutes;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@PreAuthorize("hasRole('SITE')")
@RequestMapping(RestRoutes.StatisticsV1.PRIVATE)
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsHandler handler;

    @GetMapping
    public ResponseEntity<StatisticsResponse> getStatistics(
            @RequestParam(name = StatisticsDateRange.START_DATE, required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = StatisticsDateRange.END_DATE, required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(handler.handleAllPlayers(StatisticsDateRange.of(startDate, endDate)));
    }

    @GetMapping(PlayerAppIdParam.PATH)
    public ResponseEntity<StatisticsResponse> getStatisticsByPlayerAppId(
            @PathVariable(PlayerAppIdParam.NAME) long playerAppId,
            @RequestParam(name = StatisticsDateRange.START_DATE, required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = StatisticsDateRange.END_DATE, required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(handler.handlePlayerByApplicationId(
                playerAppId,
                StatisticsDateRange.of(startDate, endDate)
        ));
    }

}
