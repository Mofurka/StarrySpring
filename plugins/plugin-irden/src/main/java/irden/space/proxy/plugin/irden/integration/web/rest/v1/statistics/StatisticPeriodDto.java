package irden.space.proxy.plugin.irden.integration.web.rest.v1.statistics;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StatisticPeriodDto(

        @JsonProperty("time_in_game")
        long timeInGame,

        int words,

        int characters,

        int messages
) {
}
