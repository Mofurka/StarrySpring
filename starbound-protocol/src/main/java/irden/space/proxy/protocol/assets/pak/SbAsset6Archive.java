package irden.space.proxy.protocol.assets.pak;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

public final class SbAsset6Archive implements Closeable {
    private static final byte[] MAGIC = "SBAsset6".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] INDEX_MAGIC = "INDEX".getBytes(StandardCharsets.US_ASCII);

    private final RandomAccessFile file;
    private final Map<String, Object> metadata;
    private final Map<String, PakIndexEntry> index;

    private SbAsset6Archive(RandomAccessFile file, Map<String, Object> metadata, Map<String, PakIndexEntry> index) {
        this.file = file;
        this.metadata = metadata;
        this.index = index;
    }

    public static SbAsset6Archive open(Path path) throws IOException {
        RandomAccessFile file = new RandomAccessFile(path.toFile(), "r");
        boolean success = false;
        try {
            SbAsset6Archive archive = load(file);
            success = true;
            return archive;
        } finally {
            if (!success) {
                file.close();
            }
        }
    }

    public Map<String, Object> metadata() {
        return metadata;
    }

    public Set<String> listPaths() {
        return Collections.unmodifiableSet(index.keySet());
    }

    public boolean contains(String path) {
        return index.containsKey(normalize(path));
    }

    public Optional<byte[]> findAsset(String path) throws IOException {
        PakIndexEntry entry = index.get(normalize(path));
        if (entry == null) {
            return Optional.empty();
        }
        return Optional.of(read(entry));
    }

    public byte[] readAsset(String path) throws IOException {
        PakIndexEntry entry = index.get(normalize(path));
        if (entry == null) {
            throw new IllegalArgumentException("Asset not found: " + path);
        }
        return read(entry);
    }

    @Override
    public void close() throws IOException {
        file.close();
    }

    private byte[] read(PakIndexEntry entry) throws IOException {
        if (entry.length() > Integer.MAX_VALUE) {
            throw new IllegalStateException("Asset is too large to be read into a byte[]: " + entry.length());
        }

        byte[] data = new byte[(int) entry.length()];
        synchronized (file) {
            file.seek(entry.offset());
            file.readFully(data);
        }
        return data;
    }

    private static SbAsset6Archive load(RandomAccessFile file) throws IOException {
        byte[] headerMagic = new byte[8];
        file.readFully(headerMagic);

        if (!matches(headerMagic, MAGIC)) {
            throw new IllegalStateException("Not a SBAsset6 archive");
        }

        long metadataOffset = file.readLong();
        file.seek(metadataOffset);

        byte[] indexMagic = new byte[5];
        file.readFully(indexMagic);
        if (!matches(indexMagic, INDEX_MAGIC)) {
            throw new IllegalStateException("SBAsset6 INDEX marker was not found");
        }

        SbonReader sbonReader = new SbonReader();
        Map<String, Object> metadata = Collections.unmodifiableMap(new LinkedHashMap<>(sbonReader.readMapValue(file)));

        long fileCount = sbonReader.readVarInt(file);
        if (fileCount > Integer.MAX_VALUE) {
            throw new IllegalStateException("SBAsset6 file index is too large: " + fileCount);
        }

        Map<String, PakIndexEntry> index = new HashMap<>((int) fileCount);
        for (long i = 0; i < fileCount; i++) {
            String path = sbonReader.readString(file).toLowerCase(Locale.ROOT);
            long offset = file.readLong();
            long length = file.readLong();
            index.put(path, new PakIndexEntry(offset, length));
        }

        return new SbAsset6Archive(file, metadata, index);
    }

    private static boolean matches(byte[] actual, byte[] expected) {
        if (actual.length != expected.length) {
            return false;
        }
        for (int i = 0; i < actual.length; i++) {
            if (actual[i] != expected[i]) {
                return false;
            }
        }
        return true;
    }

    private static String normalize(String path) {
        return path.toLowerCase(Locale.ROOT);
    }
}
