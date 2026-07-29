package irden.space.proxy.plugin.irden.weather;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

@Component
public class WeatherPresentationService {

    private static final Pattern DISCORD_EMOTE = Pattern.compile(":\\w+:");
    private static final Pattern MARKDOWN = Pattern.compile("[*_~`]");

    private final Random random = new Random();

    public WeatherSnapshot createSnapshot(
            WeatherConfig config,
            WeatherRuntimeState state,
            WeatherPeriodResolver.ResolvedPeriod period,
            boolean stateChanged,
            Instant now
    ) {
        WeatherConfig.Definition definition = config.states().get(state.stateId());
        if (definition == null) {
            throw new IllegalStateException("Unknown weather state: " + state.stateId());
        }

        WeatherConfig.Presentation presentation =
                choosePresentation(definition, period.id());

        String richDescription = chooseText(presentation, state.stateId());
        String plainDescription = MARKDOWN.matcher(
                DISCORD_EMOTE.matcher(richDescription).replaceAll("")
        ).replaceAll("").trim();

        return new WeatherSnapshot(
                state.stateId(),
                definition.serverWeather(),
                period.id(),
                period.config().channelName(),
                richDescription,
                plainDescription,
                presentation.image(),
                presentation.color(),
                presentation.stats(),
                state.atmosphere(),
                state.remainingTicks(),
                stateChanged,
                now
        );
    }

    private WeatherConfig.Presentation choosePresentation(
            WeatherConfig.Definition definition,
            String period
    ) {
        List<WeatherConfig.Presentation> candidates =
                definition.presentations().getOrDefault(period, List.of());

        if (candidates.isEmpty()) {
            candidates = definition.presentations().getOrDefault("*", List.of());
        }

        if (candidates.isEmpty()) {
            candidates = definition.presentations().values()
                    .stream()
                    .flatMap(List::stream)
                    .toList();
        }

        if (candidates.isEmpty()) {
            return new WeatherConfig.Presentation(
                    1,
                    "FFFF00",
                    "",
                    List.of("Погода: " + definition.serverWeather()),
                    Map.of()
            );
        }

        double total = candidates.stream()
                .mapToDouble(WeatherConfig.Presentation::weight)
                .sum();

        double roll = random.nextDouble() * total;

        for (WeatherConfig.Presentation candidate : candidates) {
            roll -= candidate.weight();
            if (roll <= 0) {
                return candidate;
            }
        }

        return candidates.getLast();
    }

    private String chooseText(
            WeatherConfig.Presentation presentation,
            String stateId
    ) {
        if (presentation.text().isEmpty()) {
            return "Погода: " + stateId;
        }

        return presentation.text().get(
                random.nextInt(presentation.text().size())
        );
    }
}
