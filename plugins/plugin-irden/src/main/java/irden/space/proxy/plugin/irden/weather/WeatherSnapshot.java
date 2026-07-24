package irden.space.proxy.plugin.irden.weather;

import java.time.Instant;
import java.util.Map;

public record WeatherSnapshot(
        String stateId,
        String serverWeather,
        String period,
        String channelName,
        String richDescription,
        String plainDescription,
        String image,
        String color,
        Map<String, Integer> stats,
        Atmosphere atmosphere,
        int remainingTicks,
        boolean stateChanged,
        Instant generatedAt
) {
    public WeatherSnapshot {
        stats = stats == null ? Map.of() : Map.copyOf(stats);
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
    }

    public int discordColor() {
        if (color == null || color.isBlank()) {
            return 0xFFFF00;
        }

        String normalized = color.trim().toLowerCase();
        if (normalized.startsWith("0x")) {
            normalized = normalized.substring(2);
        }
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }

        try {
            return Integer.parseInt(normalized, 16);
        } catch (NumberFormatException _) {
            return 0xFFFF00;
        }
    }
}
