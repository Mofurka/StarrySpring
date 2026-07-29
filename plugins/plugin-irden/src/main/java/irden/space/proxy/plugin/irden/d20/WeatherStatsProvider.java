package irden.space.proxy.plugin.irden.d20;

import irden.space.proxy.plugin.irden.weather.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class WeatherStatsProvider {
    private final WeatherService weatherService;


    public Map<String, Integer> current() {
        return weatherService.currentSnapshot().stats();
    }
}
