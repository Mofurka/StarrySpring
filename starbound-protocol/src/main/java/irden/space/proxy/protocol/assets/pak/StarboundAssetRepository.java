package irden.space.proxy.protocol.assets.pak;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public final class StarboundAssetRepository implements Closeable {
    private final List<SbAsset6Archive> archives;

    private StarboundAssetRepository(List<SbAsset6Archive> archives) {
        this.archives = List.copyOf(archives);
    }

    public static StarboundAssetRepository open(List<Path> archives) throws IOException {
        List<SbAsset6Archive> opened = new ArrayList<>(archives.size());
        boolean success = false;
        try {
            for (Path archive : archives) {
                opened.add(SbAsset6Archive.open(archive));
            }
            success = true;
            return new StarboundAssetRepository(opened);
        } finally {
            if (!success) {
                for (SbAsset6Archive archive : opened) {
                    archive.close();
                }
            }
        }
    }

    public List<SbAsset6Archive> archives() {
        return Collections.unmodifiableList(archives);
    }

    public Set<String> listPaths() {
        Set<String> paths = new LinkedHashSet<>();
        for (SbAsset6Archive archive : archives) {
            paths.addAll(archive.listPaths());
        }
        return paths;
    }

    public Optional<byte[]> findAsset(String path) throws IOException {
        for (int i = archives.size() - 1; i >= 0; i--) {
            Optional<byte[]> data = archives.get(i).findAsset(path);
            if (data.isPresent()) {
                return data;
            }
        }
        return Optional.empty();
    }

    public byte[] readAsset(String path) throws IOException {
        Optional<byte[]> data = findAsset(path);
        if (data.isPresent()) {
            return data.get();
        }
        throw new IllegalArgumentException("Asset not found in mounted archives: " + path);
    }

    @Override
    public void close() throws IOException {
        IOException first = null;
        for (SbAsset6Archive archive : archives) {
            try {
                archive.close();
            } catch (IOException e) {
                if (first == null) {
                    first = e;
                }
            }
        }
        if (first != null) {
            throw first;
        }
    }
}
