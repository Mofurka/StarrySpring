package irden.space.proxy.plugin.irden.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WeatherConfig(
        Settings settings,
        Map<String, Period> periods,
        Map<String, Definition> states,
        List<Transition> globalTransitions
) {
    public WeatherConfig {
        periods = safeMap(periods);
        states = safeMap(states);
        globalTransitions = safeList(globalTransitions);
    }

    private static <T> List<T> safeList(List<T> value) {
        return value == null ? List.of() : List.copyOf(value);
    }

    private static <T> Set<T> safeSet(Set<T> value) {
        return value == null ? Set.of() : Set.copyOf(value);
    }

    private static <K, V> Map<K, V> safeMap(Map<K, V> value) {
        return value == null ? Map.of() : Map.copyOf(value);
    }

    public record Settings(
            String defaultState,
            int historySize,
            double sameStateMultiplier,
            Map<String, Double> initialAtmosphere,
            Map<String, Range> atmosphereBounds
    ) {
        public Settings {
            historySize = historySize <= 0 ? 24 : historySize;
            sameStateMultiplier = sameStateMultiplier <= 0 ? 1.0 : sameStateMultiplier;
            initialAtmosphere = safeMap(initialAtmosphere);
            atmosphereBounds = safeMap(atmosphereBounds);
        }
    }

    public record Period(
            int fromHour,
            int toHour,
            String channelName
    ) {
    }

    public record Definition(
            String serverWeather,
            Set<String> tags,
            Set<String> allowedPeriods,
            int cooldownTicks,
            Duration duration,
            Map<String, Double> atmosphereChanges,
            List<Transition> transitions,
            Map<String, List<Presentation>> presentations
    ) {
        public Definition {
            tags = safeSet(tags);
            allowedPeriods = safeSet(allowedPeriods);
            cooldownTicks = Math.max(0, cooldownTicks);
            duration = duration == null ? new Duration(1, 1) : duration;
            atmosphereChanges = safeMap(atmosphereChanges);
            transitions = safeList(transitions);
            presentations = safeMap(presentations);
        }
    }

    public record Duration(
            int minTicks,
            int maxTicks
    ) {
        public Duration {
            minTicks = Math.max(1, minTicks);
            maxTicks = Math.max(minTicks, maxTicks);
        }
    }

    public record Transition(
            String to,
            double weight,
            Conditions conditions,
            List<WeightModifier> modifiers
    ) {
        public Transition {
            conditions = conditions == null ? Conditions.empty() : conditions;
            modifiers = safeList(modifiers);
        }
    }

    public record WeightModifier(
            Conditions condition,
            double multiplier
    ) {
        public WeightModifier {
            condition = condition == null ? Conditions.empty() : condition;
        }
    }

    public record Conditions(
            Set<String> periods,
            Set<String> currentAnyTags,
            Set<String> currentAllTags,
            Map<String, Range> atmosphere,
            HistoryCondition recentWeather,
            HistoryCondition absentRecentWeather
    ) {
        public Conditions {
            periods = safeSet(periods);
            currentAnyTags = safeSet(currentAnyTags);
            currentAllTags = safeSet(currentAllTags);
            atmosphere = safeMap(atmosphere);
        }

        public static Conditions empty() {
            return new Conditions(Set.of(), Set.of(), Set.of(), Map.of(), null, null);
        }
    }

    public record HistoryCondition(
            Set<String> anyTags,
            Set<String> allTags,
            Set<String> anyStates,
            int withinTicks
    ) {
        public HistoryCondition {
            anyTags = safeSet(anyTags);
            allTags = safeSet(allTags);
            anyStates = safeSet(anyStates);
            withinTicks = Math.max(1, withinTicks);
        }
    }

    public record Range(
            Double min,
            Double max
    ) {
        public boolean contains(double value) {
            return (min == null || value >= min) && (max == null || value <= max);
        }

        public double clamp(double value) {
            double result = value;
            if (min != null) {
                result = Math.max(min, result);
            }
            if (max != null) {
                result = Math.min(max, result);
            }
            return result;
        }
    }

    public record Presentation(
            double weight,
            String color,
            String image,
            List<String> text,
            Map<String, Integer> stats
    ) {
        public Presentation {
            weight = weight <= 0 ? 1.0 : weight;
            text = safeList(text);
            stats = safeMap(stats);
        }
    }
}
