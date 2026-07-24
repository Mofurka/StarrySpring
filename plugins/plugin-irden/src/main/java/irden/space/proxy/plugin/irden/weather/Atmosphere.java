package irden.space.proxy.plugin.irden.weather;

import java.util.HashMap;
import java.util.Map;

public record Atmosphere(Map<String, Double> values) {

    public Atmosphere {
        values = values == null ? Map.of() : Map.copyOf(values);
    }

    public static Atmosphere initial(WeatherConfig.Settings settings) {
        return new Atmosphere(settings.initialAtmosphere());
    }

    public double value(String name) {
        return values.getOrDefault(name, 0.0);
    }

    public Atmosphere apply(
            Map<String, Double> changes,
            Map<String, WeatherConfig.Range> bounds
    ) {
        if (changes == null || changes.isEmpty()) {
            return this;
        }

        Map<String, Double> updated = new HashMap<>(values);
        changes.forEach((name, delta) -> {
            double value = updated.getOrDefault(name, 0.0) + delta;
            WeatherConfig.Range range = bounds.get(name);
            updated.put(name, range == null ? value : range.clamp(value));
        });

        return new Atmosphere(updated);
    }
}
