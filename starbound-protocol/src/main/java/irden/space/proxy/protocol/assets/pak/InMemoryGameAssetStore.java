package irden.space.proxy.protocol.assets.pak;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import irden.space.proxy.protocol.assets.item.ActiveItem;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public final class InMemoryGameAssetStore implements GameAssetStore {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().configure(JsonParser.Feature.ALLOW_COMMENTS, true);

    private final boolean enabled;
    private final List<Path> archives;
    private final Set<String> excludedExtensions;

    private volatile Map<String, byte[]> assets = Collections.emptyMap();
    private volatile Map<String, ActiveItem> itemDatabase = Collections.emptyMap();
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
            itemDatabase = Collections.emptyMap();
            initialized = true;
            return;
        }

        if (archives.isEmpty()) {
            throw new IllegalStateException("assets.store.archives must contain at least one archive path when enabled");
        }

        try (StarboundAssetRepository repository = StarboundAssetRepository.open(archives)) {
            Map<String, byte[]> loaded = new LinkedHashMap<>();
            Map<String, ActiveItem> items = new LinkedHashMap<>();
            int skippedItems = 0;

            for (String path : repository.listPaths()) {
                String normalizedPath = normalizePath(path);
                if (isExcluded(normalizedPath)) {
                    continue;
                }
                byte[] data = repository.readAsset(normalizedPath);
                loaded.put(normalizedPath, data);
                
                if (normalizedPath.endsWith(".activeitem")) {
                    try {
                        JsonNode jsonNode = OBJECT_MAPPER.readTree(data);
                        JsonNode itemNameNode = jsonNode.get("itemName");

                        if (itemNameNode != null && itemNameNode.isTextual()) {
                            String itemName = itemNameNode.asText();
                            Path itemPath = Path.of(normalizedPath);
                            String itemDirectory = itemPath.getParent().toString();
                            ActiveItem activeItem = new ActiveItem(itemName, jsonNode, itemDirectory);
                            items.put(itemName, activeItem);

                        } else {
                            skippedItems++;
                        }
                    } catch (Exception e) {
                        skippedItems++;
                    }
                }
            }

            assets = Collections.unmodifiableMap(loaded);
            itemDatabase = Collections.unmodifiableMap(items);
            initialized = true;

            if (skippedItems > 0) {
                System.err.println("Warning: Skipped " + skippedItems + " .activeitem files due to missing itemName or parse errors");
            }
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

    @Override
    public Map<String, ActiveItem> getItemDatabase() {
        return itemDatabase;
    }

    @Override
    public Optional<ActiveItem> findItem(String itemName) {
        return Optional.ofNullable(itemDatabase.get(itemName));
    }

    @Override
    public int itemCount() {
        return itemDatabase.size();
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
