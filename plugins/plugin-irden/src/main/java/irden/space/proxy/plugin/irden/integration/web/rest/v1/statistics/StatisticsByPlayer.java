package irden.space.proxy.plugin.irden.integration.web.rest.v1.statistics;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Map;

public record StatisticsByPlayer(

        @JsonProperty("last_seen")
        LocalDateTime lastSeen,

        Map<Integer, Map<Month, StatisticPeriodDto>> statistics,

        List<String> uuids,

        boolean online,

        Long money
) {
}
