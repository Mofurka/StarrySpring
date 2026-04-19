package irden.space.boot;

import irden.space.proxy.protocol.assets.pak.GameAssetStore;
import irden.space.proxy.protocol.assets.pak.GameAssetStores;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
@RequiredArgsConstructor
public class GameAssetStoreStartupRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(GameAssetStoreStartupRunner.class);
    private final GameAssetStore gameAssetStore;

    @Override
    public void run(String... args) {
        long startTimeNanos = System.nanoTime();
        log.info("Initializing GameAssetStore...");
        gameAssetStore.initialize();
        GameAssetStores.setDefault(gameAssetStore);
        long durationMillis = (System.nanoTime() - startTimeNanos) / 1_000_000L;
        log.info("GameAssetStore initialized with {} assets ({} items) in {} ms",
                gameAssetStore.size(), gameAssetStore.itemCount(), durationMillis);
    }
}
