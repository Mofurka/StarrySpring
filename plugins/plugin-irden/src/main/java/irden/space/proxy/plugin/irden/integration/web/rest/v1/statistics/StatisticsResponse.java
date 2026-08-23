package irden.space.proxy.plugin.irden.integration.web.rest.v1.statistics;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Map;

public record StatisticsResponse(

        @JsonValue
        Map<Long, StatisticsByPlayer> players
) {
}
