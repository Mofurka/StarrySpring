package irden.space.proxy.plugin.irden.weather;

import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;

@Component
public class WeatherPeriodResolver {

    public ResolvedPeriod resolve(WeatherConfig config, ZonedDateTime now) {
        int hour = now.getHour();

        return config.periods().entrySet()
                .stream()
                .filter(entry -> contains(entry.getValue(), hour))
                .map(entry -> new ResolvedPeriod(entry.getKey(), entry.getValue()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No configured weather period contains hour " + hour
                ));
    }

    private boolean contains(WeatherConfig.Period period, int hour) {
        int from = period.fromHour();
        int to = period.toHour();

        if (from < to) {
            return hour >= from && hour < to;
        }
        return hour >= from || hour < to;
    }

    public record ResolvedPeriod(
            String id,
            WeatherConfig.Period config
    ) {
    }
}
