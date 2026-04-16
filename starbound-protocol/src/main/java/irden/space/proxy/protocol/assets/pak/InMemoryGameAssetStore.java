package irden.space.proxy.protocol.assets.pak;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class InMemoryGameAssetStore implements GameAssetStore {
    private final boolean enabled;
    private final List<Path> archives;
    private final Set<String> excludedExtensions;

    private volatile Map<String, byte[]> assets = Collections.emptyMap();
    private volatile boolean initialized = false;

    public InMemoryGameAssetStore(boolean enabled, List<Path> archives, Collection<String> excludedExtensions) {
        this.enabled = enabled;
        this.archives = List.copyOf(archives);
        this.excludedExtensions = normalizeExtensions(excludedExtensions);
    }

    @Override
    public synchronized void initialize() {
        if (!enabled) {
            assets = Collections.emptyMap();
            initialized = true;
            return;
        }

        if (archives.isEmpty()) {
            throw new IllegalStateException("assets.store.archives must contain at least one archive path when enabled");
        }

        try (StarboundAssetRepository repository = StarboundAssetRepository.open(archives)) {
            Map<String, byte[]> loaded = new LinkedHashMap<>();

            for (String path : repository.listPaths()) {
                String normalizedPath = normalizePath(path);
                if (isExcluded(normalizedPath)) {
                    continue;
                }
                loaded.put(normalizedPath, repository.readAsset(normalizedPath));
            }

            assets = Collections.unmodifiableMap(loaded);
            initialized = true;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize in-memory asset store", e);
        }
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public Set<String> listPaths() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(assets.keySet()));
    }

    @Override
    public Optional<byte[]> findAsset(String path) {
        byte[] data = assets.get(normalizePath(path));
        if (data == null) {
            return Optional.empty();
        }
        return Optional.of(data.clone());
    }

    @Override
    public byte[] readAsset(String path) {
        return findAsset(path)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found in memory: " + path));
    }

    @Override
    public int size() {
        return assets.size();
    }

    private boolean isExcluded(String path) {
        for (String extension : excludedExtensions) {
            if (path.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> normalizeExtensions(Collection<String> rawExtensions) {
        if (rawExtensions == null || rawExtensions.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> normalized = new LinkedHashSet<>();
        for (String extension : rawExtensions) {
            if (extension == null || extension.isBlank()) {
                continue;
            }
            String trimmed = extension.trim().toLowerCase(Locale.ROOT);
            normalized.add(trimmed.startsWith(".") ? trimmed : "." + trimmed);
        }
        return Collections.unmodifiableSet(normalized);
    }

    private static String normalizePath(String path) {
        return path.toLowerCase(Locale.ROOT);
    }

    public static List<Path> toPaths(Collection<String> paths) {
        if (paths == null || paths.isEmpty()) {
            return List.of();
        }
        List<Path> parsed = new ArrayList<>(paths.size());
        for (String path : paths) {
            parsed.add(Path.of(path));
        }
        return List.copyOf(parsed);
    }
}
