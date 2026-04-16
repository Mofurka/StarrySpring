package irden.space.proxy.protocol.assets.pak;

import irden.space.proxy.protocol.assets.item.ActiveItem;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface GameAssetStore {
    void initialize();

    boolean isInitialized();

    Set<String> listPaths();

    Optional<byte[]> findAsset(String path);

    byte[] readAsset(String path);

    int size();


    Map<String, ActiveItem> getItemDatabase();


    Optional<ActiveItem> findItem(String itemName);


    int itemCount();
}
