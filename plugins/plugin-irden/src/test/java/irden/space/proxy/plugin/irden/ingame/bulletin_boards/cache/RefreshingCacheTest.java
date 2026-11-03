package irden.space.proxy.plugin.irden.ingame.bulletin_boards.cache;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class RefreshingCacheTest {

    private static final Duration REFRESH_AFTER = Duration.ofSeconds(10);
    private static final Duration EXPIRE_AFTER = Duration.ofSeconds(60);

    private final AtomicLong now = new AtomicLong();
    private final List<CompletableFuture<String>> loads = new ArrayList<>();
    private final RefreshingCache<String, String> cache = new RefreshingCache<>(
            "test", REFRESH_AFTER, EXPIRE_AFTER, key -> {
        CompletableFuture<String> load = new CompletableFuture<>();
        loads.add(load);
        return load;
    }, now::get);

    private void advance(Duration duration) {
        now.addAndGet(duration.toNanos());
    }

    @Test
    void concurrentMissesShareOneLoad() {
        CompletableFuture<String> first = cache.get("board");
        CompletableFuture<String> second = cache.get("board");

        assertEquals(1, loads.size());
        loads.getFirst().complete("posts");

        assertEquals("posts", first.join());
        assertEquals("posts", second.join());
    }

    @Test
    void freshValueIsServedWithoutLoading() {
        cache.get("board");
        loads.getFirst().complete("posts");

        advance(REFRESH_AFTER.minusSeconds(1));

        assertEquals("posts", cache.get("board").join());
        assertEquals(1, loads.size());
    }

    @Test
    void staleValueIsServedWhileOneRefreshRuns() {
        cache.get("board");
        loads.getFirst().complete("old");
        advance(REFRESH_AFTER);

        assertEquals("old", cache.get("board").join());
        assertEquals("old", cache.get("board").join());
        assertEquals(2, loads.size(), "stale reads must start exactly one refresh");

        loads.get(1).complete("new");

        assertEquals("new", cache.get("board").join());
        assertEquals(2, loads.size());
    }

    @Test
    void expiredValueIsNotServed() {
        cache.get("board");
        loads.getFirst().complete("old");
        advance(EXPIRE_AFTER);

        CompletableFuture<String> read = cache.get("board");

        assertFalse(read.isDone(), "expired value must wait for a fresh load");
        loads.get(1).complete("new");
        assertEquals("new", read.join());
    }

    @Test
    void failedLoadIsNotCached() {
        CompletableFuture<String> failed = cache.get("board");
        loads.getFirst().completeExceptionally(new IllegalStateException("discord is down"));

        assertTrue(failed.isCompletedExceptionally());

        CompletableFuture<String> retry = cache.get("board");
        assertEquals(2, loads.size());
        loads.get(1).complete("posts");
        assertEquals("posts", retry.join());
    }

    @Test
    void failedRefreshKeepsServingStaleValue() {
        cache.get("board");
        loads.getFirst().complete("old");
        advance(REFRESH_AFTER);

        cache.get("board");
        loads.get(1).completeExceptionally(new IllegalStateException("discord is down"));

        assertEquals("old", cache.get("board").join());
        assertEquals(3, loads.size(), "next stale read should try to refresh again");
    }

    @Test
    void reloadMakesReadersWaitForFreshData() {
        cache.get("board");
        loads.getFirst().complete("before edit");

        cache.reload("board");
        CompletableFuture<String> read = cache.get("board");

        assertFalse(read.isDone());
        loads.get(1).complete("after edit");
        assertEquals("after edit", read.join());
    }

    @Test
    void reloadIgnoresKeysNobodyReads() {
        cache.reload("board");

        assertTrue(loads.isEmpty());
    }

    @Test
    void refreshStartedBeforeReloadDoesNotOverwriteIt() {
        cache.get("board");
        loads.getFirst().complete("old");
        advance(REFRESH_AFTER);

        cache.get("board");
        cache.reload("board");
        loads.get(2).complete("after edit");
        loads.get(1).complete("stale refresh");

        assertEquals("after edit", cache.get("board").join());
    }

    @Test
    void invalidateDropsValue() {
        cache.get("board");
        loads.getFirst().complete("posts");

        cache.invalidate("board");
        cache.get("board");

        assertEquals(2, loads.size());
    }

    @Test
    void callersCannotCompleteSharedValue() {
        CompletableFuture<String> read = cache.get("board");
        read.complete("hijacked");

        CompletableFuture<String> other = cache.get("board");
        loads.getFirst().complete("posts");

        assertEquals("posts", other.join());
    }
}
