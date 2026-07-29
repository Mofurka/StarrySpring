package irden.space.proxy.plugin.irden.weather;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public record WeatherRuntimeState(
        String stateId,
        Atmosphere atmosphere,
        int remainingTicks,
        Instant startedAt,
        List<HistoryEntry> history
) {
    public WeatherRuntimeState {
        remainingTicks = Math.max(1, remainingTicks);
        startedAt = startedAt == null ? Instant.now() : startedAt;
        history = history == null ? List.of() : List.copyOf(history);
    }

    public record HistoryEntry(
            String stateId,
            Set<String> tags,
            Instant completedAt
    ) {
        public HistoryEntry {
            tags = tags == null ? Set.of() : Set.copyOf(tags);
            completedAt = completedAt == null ? Instant.now() : completedAt;
        }
    }
}
