package irden.space.proxy.plugin.irden.weather;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class WeatherConfigValidator {

    public void validate(WeatherConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Weather config is null");
        }
        if (config.settings() == null) {
            throw new IllegalArgumentException("Weather settings are missing");
        }
        if (config.states().isEmpty()) {
            throw new IllegalArgumentException("No weather states are configured");
        }
        if (!config.states().containsKey(config.settings().defaultState())) {
            throw new IllegalArgumentException(
                    "Default weather state does not exist: " + config.settings().defaultState()
            );
        }

        validatePeriods(config.periods());

        config.states().forEach((stateId, definition) -> {
            if (stateId == null || stateId.isBlank()) {
                throw new IllegalArgumentException("Weather state has an empty id");
            }
            if (definition.serverWeather() == null || definition.serverWeather().isBlank()) {
                throw new IllegalArgumentException(
                        "serverWeather is empty for state: " + stateId
                );
            }

            definition.allowedPeriods().forEach(period -> requirePeriod(config, stateId, period));
            definition.presentations().keySet().stream()
                    .filter(period -> !"*".equals(period))
                    .forEach(period -> requirePeriod(config, stateId, period));

            validateTransitions(config, stateId, definition.transitions());
        });

        validateTransitions(config, "<global>", config.globalTransitions());
    }

    private void validatePeriods(Map<String, WeatherConfig.Period> periods) {
        if (periods.isEmpty()) {
            throw new IllegalArgumentException("No periods are configured");
        }

        periods.forEach((id, period) -> {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("Period has an empty id");
            }
            if (period.fromHour() < 0 || period.fromHour() > 23) {
                throw new IllegalArgumentException("Invalid fromHour for period: " + id);
            }
            if (period.toHour() < 1 || period.toHour() > 24) {
                throw new IllegalArgumentException("Invalid toHour for period: " + id);
            }
            if (period.fromHour() == period.toHour()) {
                throw new IllegalArgumentException("Period has zero duration: " + id);
            }
        });

        for (int hour = 0; hour < 24; hour++) {
            int matches = 0;
            for (WeatherConfig.Period period : periods.values()) {
                if (containsHour(period, hour)) {
                    matches++;
                }
            }
            if (matches != 1) {
                throw new IllegalArgumentException(
                        "Hour " + hour + " must belong to exactly one period, actual: " + matches
                );
            }
        }
    }

    private boolean containsHour(WeatherConfig.Period period, int hour) {
        int from = period.fromHour();
        int to = period.toHour();

        if (from < to) {
            return hour >= from && hour < to;
        }
        return hour >= from || hour < to;
    }

    private void validateTransitions(
            WeatherConfig config,
            String source,
            List<WeatherConfig.Transition> transitions
    ) {
        for (WeatherConfig.Transition transition : transitions) {
            if (transition.to() == null || !config.states().containsKey(transition.to())) {
                throw new IllegalArgumentException(
                        "Unknown transition target '%s' from '%s'"
                                .formatted(transition.to(), source)
                );
            }
            if (transition.weight() < 0) {
                throw new IllegalArgumentException(
                        "Negative transition weight from '%s' to '%s'"
                                .formatted(source, transition.to())
                );
            }

            validateConditions(config, source, transition.conditions());
            transition.modifiers().forEach(modifier -> {
                if (modifier.multiplier() < 0) {
                    throw new IllegalArgumentException(
                            "Negative weight multiplier from '%s' to '%s'"
                                    .formatted(source, transition.to())
                    );
                }
                validateConditions(config, source, modifier.condition());
            });
        }
    }

    private void validateConditions(
            WeatherConfig config,
            String source,
            WeatherConfig.Conditions conditions
    ) {
        conditions.periods().forEach(period -> requirePeriod(config, source, period));
    }

    private void requirePeriod(WeatherConfig config, String stateId, String period) {
        if (!config.periods().containsKey(period)) {
            throw new IllegalArgumentException(
                    "Unknown period '%s' in state/rule '%s'".formatted(period, stateId)
            );
        }
    }
}
