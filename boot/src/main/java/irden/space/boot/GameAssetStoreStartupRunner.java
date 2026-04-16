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
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final GameAssetStore gameAssetStore;

    @Override
    public void run(String... args) {
        var startTime = System.currentTimeMillis();
        log.info("Initializing GameAssetStore...");
        gameAssetStore.initialize();
        GameAssetStores.setDefault(gameAssetStore);
        var endTime = System.currentTimeMillis();
        var duration = endTime - startTime;
        log.info("GameAssetStore initialized with {} assets in {} ms", gameAssetStore.size(), duration);
    }
}
