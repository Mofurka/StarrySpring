package irden.space.proxy.plugin.irden.ingame.bulletin_boards.cache;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.LongSupplier;


@Slf4j
final class RefreshingCache<K, V> {

    private static final Duration LOAD_TIMEOUT = Duration.ofSeconds(15);

    private final String name;
    private final long refreshAfterNanos;
    private final long expireAfterNanos;
    private final Function<K, CompletableFuture<V>> loader;
    private final LongSupplier ticker;
    private final Map<K, Entry<V>> entries = new ConcurrentHashMap<>();

    RefreshingCache(
            String name,
            Duration refreshAfter,
            Duration expireAfter,
            Function<K, CompletableFuture<V>> loader
    ) {
        this(name, refreshAfter, expireAfter, loader, System::nanoTime);
    }

    RefreshingCache(
            String name,
            Duration refreshAfter,
            Duration expireAfter,
            Function<K, CompletableFuture<V>> loader,
            LongSupplier ticker
    ) {
        if (refreshAfter.compareTo(expireAfter) > 0) {
            throw new IllegalArgumentException("refreshAfter must not exceed expireAfter");
        }
        this.name = Objects.requireNonNull(name, "name");
        this.refreshAfterNanos = refreshAfter.toNanos();
        this.expireAfterNanos = expireAfter.toNanos();
        this.loader = Objects.requireNonNull(loader, "loader");
        this.ticker = Objects.requireNonNull(ticker, "ticker");
    }

    CompletableFuture<V> get(K key) {
        long now = ticker.getAsLong();
        Entry<V> entry = entries.get(key);

        if (entry == null || entry.expired(now, expireAfterNanos)) {
            entry = load(key, now);
        } else if (entry.stale(now, refreshAfterNanos) && entry.refreshing.compareAndSet(false, true)) {
            refresh(key, entry, now);
        }

        return entry.value.copy();
    }

    void reload(K key) {
        if (!entries.containsKey(key)) {
            return;
        }

        Entry<V> created = new Entry<>(ticker.getAsLong());
        if (entries.computeIfPresent(key, (ignored, current) -> created) == created) {
            start(key, created);
        }
    }

    void invalidate(K key) {
        entries.remove(key);
    }

    private Entry<V> load(K key, long now) {
        Entry<V> created = new Entry<>(now);
        Entry<V> winner = entries.compute(key, (ignored, current) ->
                current == null || current.expired(now, expireAfterNanos) ? created : current);

        if (winner == created) {
            entries.values().removeIf(other -> other.expired(now, expireAfterNanos));
            start(key, created);
        }
        return winner;
    }

    private void start(K key, Entry<V> entry) {
        loadSafely(key).whenComplete((value, failure) -> {
            if (failure != null) {
                entries.remove(key, entry);
                entry.value.completeExceptionally(failure);
            } else {
                entry.value.complete(value);
            }
        });
    }

    private void refresh(K key, Entry<V> stale, long startedAt) {
        loadSafely(key).whenComplete((value, failure) -> {
            if (failure != null) {
                log.warn("Failed to refresh {} for {}, serving stale value", name, key, failure);
                stale.refreshing.set(false);
                return;
            }
            entries.replace(key, stale, Entry.loaded(value, startedAt));
        });
    }

    private CompletableFuture<V> loadSafely(K key) {
        try {
            return loader.apply(key)
                    .copy()
                    .orTimeout(LOAD_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private static final class Entry<V> {
        private final CompletableFuture<V> value;
        private final long loadedAt;
        private final AtomicBoolean refreshing = new AtomicBoolean();

        private Entry(long loadedAt) {
            this(new CompletableFuture<>(), loadedAt);
        }

        private Entry(CompletableFuture<V> value, long loadedAt) {
            this.value = value;
            this.loadedAt = loadedAt;
        }

        private static <V> Entry<V> loaded(V value, long loadedAt) {
            return new Entry<>(CompletableFuture.completedFuture(value), loadedAt);
        }

        private boolean stale(long now, long refreshAfterNanos) {
            return value.isDone() && !value.isCompletedExceptionally() && now - loadedAt >= refreshAfterNanos;
        }

        private boolean expired(long now, long expireAfterNanos) {
            return value.isDone() && (value.isCompletedExceptionally() || now - loadedAt >= expireAfterNanos);
        }
    }
}
