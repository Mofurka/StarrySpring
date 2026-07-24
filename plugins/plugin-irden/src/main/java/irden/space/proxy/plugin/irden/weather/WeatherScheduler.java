package irden.space.proxy.plugin.irden.weather;

import irden.space.proxy.plugin.api.annotations.OnLoad;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class WeatherScheduler {

    private final WeatherService weatherService;
    private final AtomicBoolean initialized = new AtomicBoolean();

    public WeatherScheduler(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @OnLoad
    public void publishInitialWeather() {
        if (initialized.compareAndSet(false, true)) {
            weatherService.publishCurrent();
        }
    }

    @Scheduled(
            cron = "${irden.weather.tick-cron:0 0 * * * *}",
            zone = "${irden.weather.zone:Europe/Amsterdam}"
    )
    public void updateWeather() {
        weatherService.tick();
    }
}
