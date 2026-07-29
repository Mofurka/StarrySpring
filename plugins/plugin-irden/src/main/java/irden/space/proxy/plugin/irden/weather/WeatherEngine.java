package irden.space.proxy.plugin.irden.weather;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Component
public class WeatherEngine {

    private final WeatherConditionEvaluator conditionEvaluator;
    private final Random random;

    @Autowired
    public WeatherEngine(WeatherConditionEvaluator conditionEvaluator) {
        this(conditionEvaluator, new Random());
    }

    WeatherEngine(WeatherConditionEvaluator conditionEvaluator, Random random) {
        this.conditionEvaluator = conditionEvaluator;
        this.random = random;
    }

    public Decision tick(
            WeatherConfig config,
            WeatherRuntimeState current,
            String period,
            Instant now
    ) {
        WeatherConfig.Definition currentDefinition =
                requireDefinition(config, current.stateId());

        Atmosphere updatedAtmosphere = current.atmosphere().apply(
                currentDefinition.atmosphereChanges(),
                config.settings().atmosphereBounds()
        );

        WeatherRuntimeState updatedCurrent = new WeatherRuntimeState(
                current.stateId(),
                updatedAtmosphere,
                current.remainingTicks(),
                current.startedAt(),
                current.history()
        );

        boolean currentAllowed = isAllowed(currentDefinition, period);

        if (currentAllowed && current.remainingTicks() > 1) {
            return new Decision(
                    new WeatherRuntimeState(
                            current.stateId(),
                            updatedAtmosphere,
                            current.remainingTicks() - 1,
                            current.startedAt(),
                            updatedHistory(
                                    config,
                                    current,
                                    currentDefinition,
                                    now
                            )
                    ),
                    false
            );
        }

        Map<String, Double> candidateWeights = new HashMap<>();

        addCandidates(
                config,
                updatedCurrent,
                currentDefinition,
                period,
                currentDefinition.transitions(),
                candidateWeights
        );

        addCandidates(
                config,
                updatedCurrent,
                currentDefinition,
                period,
                config.globalTransitions(),
                candidateWeights
        );

        String selectedState = candidateWeights.isEmpty()
                ? fallbackState(config, current, period)
                : choose(candidateWeights);

        WeatherConfig.Definition selectedDefinition =
                requireDefinition(config, selectedState);

        List<WeatherRuntimeState.HistoryEntry> history =
                updatedHistory(config, current, currentDefinition, now);

        WeatherRuntimeState next = new WeatherRuntimeState(
                selectedState,
                updatedAtmosphere,
                randomDuration(selectedDefinition.duration()),
                now,
                history
        );

        return new Decision(next, !selectedState.equals(current.stateId()));
    }

    public WeatherRuntimeState createInitial(
            WeatherConfig config,
            String stateId,
            Instant now
    ) {
        WeatherConfig.Definition definition = requireDefinition(config, stateId);

        return new WeatherRuntimeState(
                stateId,
                Atmosphere.initial(config.settings()),
                randomDuration(definition.duration()),
                now,
                List.of()
        );
    }

    public WeatherRuntimeState force(
            WeatherConfig config,
            WeatherRuntimeState current,
            String stateId,
            Instant now
    ) {
        WeatherConfig.Definition definition = requireDefinition(config, stateId);
        WeatherConfig.Definition oldDefinition =
                requireDefinition(config, current.stateId());

        return new WeatherRuntimeState(
                stateId,
                current.atmosphere(),
                randomDuration(definition.duration()),
                now,
                updatedHistory(config, current, oldDefinition, now)
        );
    }

    private void addCandidates(
            WeatherConfig config,
            WeatherRuntimeState current,
            WeatherConfig.Definition currentDefinition,
            String period,
            List<WeatherConfig.Transition> rules,
            Map<String, Double> candidateWeights
    ) {
        for (WeatherConfig.Transition rule : rules) {
            WeatherConfig.Definition target = requireDefinition(config, rule.to());

            if (!isAllowed(target, period)) {
                continue;
            }
            if (isOnCooldown(rule.to(), target, current, currentDefinition)) {
                continue;
            }
            if (!conditionEvaluator.matches(
                    rule.conditions(),
                    period,
                    current,
                    currentDefinition
            )) {
                continue;
            }

            double weight = rule.weight();

            for (WeatherConfig.WeightModifier modifier : rule.modifiers()) {
                if (conditionEvaluator.matches(
                        modifier.condition(),
                        period,
                        current,
                        currentDefinition
                )) {
                    weight *= modifier.multiplier();
                }
            }

            if (rule.to().equals(current.stateId())) {
                weight *= config.settings().sameStateMultiplier();
            }

            if (weight > 0) {
                candidateWeights.merge(rule.to(), weight, Double::sum);
            }
        }
    }

    private boolean isOnCooldown(
            String targetState,
            WeatherConfig.Definition targetDefinition,
            WeatherRuntimeState current,
            WeatherConfig.Definition currentDefinition
    ) {
        int cooldown = targetDefinition.cooldownTicks();
        if (cooldown <= 0) {
            return false;
        }

        if (current.stateId().equals(targetState)) {
            return true;
        }

        return current.history()
                .stream()
                .limit(cooldown)
                .anyMatch(entry -> entry.stateId().equals(targetState));
    }

    private List<WeatherRuntimeState.HistoryEntry> updatedHistory(
            WeatherConfig config,
            WeatherRuntimeState current,
            WeatherConfig.Definition currentDefinition,
            Instant now
    ) {
        List<WeatherRuntimeState.HistoryEntry> result = new ArrayList<>();
        result.add(new WeatherRuntimeState.HistoryEntry(
                current.stateId(),
                currentDefinition.tags(),
                now
        ));
        result.addAll(current.history());

        return result.stream()
                .limit(config.settings().historySize())
                .toList();
    }

    private boolean isAllowed(
            WeatherConfig.Definition definition,
            String period
    ) {
        return definition.allowedPeriods().isEmpty()
                || definition.allowedPeriods().contains(period);
    }

    private String fallbackState(
            WeatherConfig config,
            WeatherRuntimeState current,
            String period
    ) {
        WeatherConfig.Definition currentDefinition =
                requireDefinition(config, current.stateId());

        if (isAllowed(currentDefinition, period)) {
            return current.stateId();
        }

        String defaultState = config.settings().defaultState();
        WeatherConfig.Definition defaultDefinition =
                requireDefinition(config, defaultState);

        if (!isAllowed(defaultDefinition, period)) {
            throw new IllegalStateException(
                    "Neither current nor default weather state is allowed in period: " + period
            );
        }

        return defaultState;
    }

    private String choose(Map<String, Double> weights) {
        double total = weights.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();

        if (total <= 0) {
            throw new IllegalStateException("Total weather transition weight is zero");
        }

        double roll = random.nextDouble() * total;

        for (Map.Entry<String, Double> entry : weights.entrySet()) {
            roll -= entry.getValue();
            if (roll <= 0) {
                return entry.getKey();
            }
        }

        return weights.keySet().iterator().next();
    }

    private int randomDuration(WeatherConfig.Duration duration) {
        if (duration.minTicks() == duration.maxTicks()) {
            return duration.minTicks();
        }

        return duration.minTicks()
                + random.nextInt(duration.maxTicks() - duration.minTicks() + 1);
    }

    private WeatherConfig.Definition requireDefinition(
            WeatherConfig config,
            String stateId
    ) {
        WeatherConfig.Definition definition = config.states().get(stateId);
        if (definition == null) {
            throw new IllegalStateException("Unknown weather state: " + stateId);
        }
        return definition;
    }

    public record Decision(
            WeatherRuntimeState state,
            boolean stateChanged
    ) {
    }
}
