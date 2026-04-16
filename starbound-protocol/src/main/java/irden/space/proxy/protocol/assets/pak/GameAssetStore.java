package irden.space.proxy.protocol.assets.pak;

import java.util.Optional;
import java.util.Set;

public interface GameAssetStore {
    void initialize();

    boolean isInitialized();

    Set<String> listPaths();

    Optional<byte[]> findAsset(String path);

    byte[] readAsset(String path);

    int size();
}
