package irden.space.proxy.protocol.assets.pak;

import irden.space.proxy.protocol.assets.item.ActiveItem;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public final class GameAssetStores {
    private static final AtomicReference<GameAssetStore> DEFAULT_STORE = new AtomicReference<>(NoOpGameAssetStore.INSTANCE);

    private GameAssetStores() {
    }

    public static GameAssetStore defaultStore() {
        return DEFAULT_STORE.get();
    }

    public static void setDefault(GameAssetStore store) {
        if (store == null) {
            throw new IllegalArgumentException("store cannot be null");
        }
        DEFAULT_STORE.set(store);
    }

    public static void clearDefault() {
        DEFAULT_STORE.set(NoOpGameAssetStore.INSTANCE);
    }

    private enum NoOpGameAssetStore implements GameAssetStore {
        INSTANCE;

        @Override
        public void initialize() {
        }

        @Override
        public boolean isInitialized() {
            return true;
        }

        @Override
        public Set<String> listPaths() {
            return Collections.emptySet();
        }

        @Override
        public Optional<byte[]> findAsset(String path) {
            return Optional.empty();
        }

        @Override
        public byte[] readAsset(String path) {
            throw new IllegalArgumentException("Asset store is not configured");
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public Map<String, ActiveItem> getItemDatabase() {
            return Map.of();
        }

        @Override
        public Optional<ActiveItem> findItem(String itemName) {
            return Optional.empty();
        }

        @Override
        public int itemCount() {
            return 0;
        }
    }
}
