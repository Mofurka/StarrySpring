package irden.space.proxy.plugin.irden.weather;

import irden.space.proxy.plugin.irden.IrdenConfig;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class WeatherService {

    private final WeatherConfigService configService;
    private final WeatherStateRepository stateRepository;
    private final WeatherPeriodResolver periodResolver;
    private final WeatherEngine engine;
    private final WeatherPresentationService presentationService;
    private final ApplicationEventPublisher eventPublisher;
    private final IrdenConfig.WeatherProperties properties;

    private final ReentrantLock lock = new ReentrantLock();
    private WeatherRuntimeState state;
    private WeatherSnapshot lastSnapshot;

    public WeatherService(
            WeatherConfigService configService,
            WeatherStateRepository stateRepository,
            WeatherPeriodResolver periodResolver,
            WeatherEngine engine,
            WeatherPresentationService presentationService,
            ApplicationEventPublisher eventPublisher,
            IrdenConfig properties
    ) {
        this.configService = configService;
        this.stateRepository = stateRepository;
        this.periodResolver = periodResolver;
        this.engine = engine;
        this.presentationService = presentationService;
        this.eventPublisher = eventPublisher;
        this.properties = properties.weather();
    }


    public WeatherSnapshot tick() {
        lock.lock();
        try {
            WeatherConfig config = configService.get();
            Instant now = Instant.now();
            WeatherPeriodResolver.ResolvedPeriod period = resolvePeriod(config);

            WeatherRuntimeState current = state(config, period, now);
            WeatherEngine.Decision decision =
                    engine.tick(config, current, period.id(), now);

            state = decision.state();
            stateRepository.save(state);

            lastSnapshot = presentationService.createSnapshot(
                    config,
                    state,
                    period,
                    decision.stateChanged(),
                    now
            );

            eventPublisher.publishEvent(new WeatherUpdatedEvent(lastSnapshot));
            return lastSnapshot;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Publishes current persisted weather without advancing the simulation.
     */
    public WeatherSnapshot publishCurrent() {
        lock.lock();
        try {
            WeatherConfig config = configService.get();
            Instant now = Instant.now();
            WeatherPeriodResolver.ResolvedPeriod period = resolvePeriod(config);
            state = state(config, period, now);
            boolean changed = resolveDisallowedState(config, period, now);

            lastSnapshot = presentationService.createSnapshot(
                    config,
                    state,
                    period,
                    changed,
                    now
            );

            eventPublisher.publishEvent(new WeatherUpdatedEvent(lastSnapshot));
            return lastSnapshot;
        } finally {
            lock.unlock();
        }
    }


    public WeatherSnapshot forceState(String stateId) {
        lock.lock();
        try {
            WeatherConfig config = configService.get();
            WeatherConfig.Definition target = config.states().get(stateId);

            if (target == null) {
                throw new IllegalArgumentException("Unknown weather state: " + stateId);
            }

            Instant now = Instant.now();
            WeatherPeriodResolver.ResolvedPeriod period = resolvePeriod(config);

            if (!target.allowedPeriods().isEmpty()
                    && !target.allowedPeriods().contains(period.id())) {
                throw new IllegalArgumentException(
                        "Weather state '%s' is not allowed in period '%s'"
                                .formatted(stateId, period.id())
                );
            }

            WeatherRuntimeState current = state(config, period, now);
            state = engine.force(config, current, stateId, now);
            stateRepository.save(state);

            lastSnapshot = presentationService.createSnapshot(
                    config,
                    state,
                    period,
                    !stateId.equals(current.stateId()),
                    now
            );

            eventPublisher.publishEvent(new WeatherUpdatedEvent(lastSnapshot));
            return lastSnapshot;
        } finally {
            lock.unlock();
        }
    }


    public WeatherSnapshot reloadConfiguration() {
        lock.lock();
        try {
            WeatherConfig config = configService.reload();
            Instant now = Instant.now();
            WeatherPeriodResolver.ResolvedPeriod period = resolvePeriod(config);

            if (state == null) {
                state = stateRepository.load().orElse(null);
            }

            if (state == null || !config.states().containsKey(state.stateId())) {
                state = engine.createInitial(
                        config,
                        config.settings().defaultState(),
                        now
                );
                stateRepository.save(state);
            }

            boolean changed = resolveDisallowedState(config, period, now);

            lastSnapshot = presentationService.createSnapshot(
                    config,
                    state,
                    period,
                    changed,
                    now
            );

            eventPublisher.publishEvent(new WeatherUpdatedEvent(lastSnapshot));
            return lastSnapshot;
        } finally {
            lock.unlock();
        }
    }

    public WeatherSnapshot currentSnapshot() {
        lock.lock();
        try {
            if (lastSnapshot == null) {
                return publishCurrent();
            }
            return lastSnapshot;
        } finally {
            lock.unlock();
        }
    }

    public Set<String> configuredStateIds() {
        return Set.copyOf(configService.get().states().keySet());
    }

    private WeatherRuntimeState state(
            WeatherConfig config,
            WeatherPeriodResolver.ResolvedPeriod period,
            Instant now
    ) {
        if (state == null) {
            state = stateRepository.load().orElse(null);
        }

        if (state == null || !config.states().containsKey(state.stateId())) {
            state = engine.createInitial(
                    config,
                    config.settings().defaultState(),
                    now
            );
            stateRepository.save(state);
        }

        WeatherConfig.Definition definition =
                config.states().get(state.stateId());

        if (!definition.allowedPeriods().isEmpty()
                && !definition.allowedPeriods().contains(period.id())) {
            // The next tick will immediately choose an allowed transition.
            state = new WeatherRuntimeState(
                    state.stateId(),
                    state.atmosphere(),
                    1,
                    state.startedAt(),
                    state.history()
            );
        }

        return state;
    }

    private boolean resolveDisallowedState(
            WeatherConfig config,
            WeatherPeriodResolver.ResolvedPeriod period,
            Instant now
    ) {
        WeatherConfig.Definition definition = config.states().get(state.stateId());

        if (definition.allowedPeriods().isEmpty()
                || definition.allowedPeriods().contains(period.id())) {
            return false;
        }

        WeatherEngine.Decision decision =
                engine.tick(config, state, period.id(), now);

        state = decision.state();
        stateRepository.save(state);
        return decision.stateChanged();
    }

    private WeatherPeriodResolver.ResolvedPeriod resolvePeriod(
            WeatherConfig config
    ) {
        ZoneId zone = ZoneId.of(properties.zone());
        return periodResolver.resolve(config, ZonedDateTime.now(zone));
    }
}
