package irden.space.proxy.plugin.irden.weather;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class WeatherConditionEvaluator {

    public boolean matches(
            WeatherConfig.Conditions conditions,
            String period,
            WeatherRuntimeState current,
            WeatherConfig.Definition currentDefinition
    ) {
        if (!conditions.periods().isEmpty() && !conditions.periods().contains(period)) {
            return false;
        }

        Set<String> currentTags = currentDefinition.tags();

        if (!conditions.currentAnyTags().isEmpty()
                && currentTags.stream().noneMatch(conditions.currentAnyTags()::contains)) {
            return false;
        }

        if (!currentTags.containsAll(conditions.currentAllTags())) {
            return false;
        }

        boolean atmosphereMatches = conditions.atmosphere().entrySet()
                .stream()
                .allMatch(entry ->
                        entry.getValue().contains(current.atmosphere().value(entry.getKey()))
                );

        if (!atmosphereMatches) {
            return false;
        }

        List<WeatherRuntimeState.HistoryEntry> timeline =
                timelineWithCurrent(current, currentDefinition);

        if (conditions.recentWeather() != null
                && !historyMatches(timeline, conditions.recentWeather())) {
            return false;
        }

        return conditions.absentRecentWeather() == null
                || !historyMatches(timeline, conditions.absentRecentWeather());
    }

    private List<WeatherRuntimeState.HistoryEntry> timelineWithCurrent(
            WeatherRuntimeState current,
            WeatherConfig.Definition currentDefinition
    ) {
        List<WeatherRuntimeState.HistoryEntry> result = new ArrayList<>();
        result.add(new WeatherRuntimeState.HistoryEntry(
                current.stateId(),
                currentDefinition.tags(),
                current.startedAt()
        ));
        result.addAll(current.history());
        return result;
    }

    private boolean historyMatches(
            List<WeatherRuntimeState.HistoryEntry> timeline,
            WeatherConfig.HistoryCondition condition
    ) {
        return timeline.stream()
                .limit(condition.withinTicks())
                .anyMatch(entry -> entryMatches(entry, condition));
    }

    private boolean entryMatches(
            WeatherRuntimeState.HistoryEntry entry,
            WeatherConfig.HistoryCondition condition
    ) {
        boolean anyTagMatches = condition.anyTags().isEmpty()
                || entry.tags().stream().anyMatch(condition.anyTags()::contains);

        boolean allTagsMatch = entry.tags().containsAll(condition.allTags());

        boolean stateMatches = condition.anyStates().isEmpty()
                || condition.anyStates().contains(entry.stateId());

        return anyTagMatches && allTagsMatch && stateMatches;
    }
}
