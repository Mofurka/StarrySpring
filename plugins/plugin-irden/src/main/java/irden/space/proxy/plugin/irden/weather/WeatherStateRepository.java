package irden.space.proxy.plugin.irden.weather;

import java.util.Optional;

public interface WeatherStateRepository {

    Optional<WeatherRuntimeState> load();

    void save(WeatherRuntimeState state);
}
