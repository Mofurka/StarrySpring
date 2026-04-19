package irden.space.proxy.protocol.payload.common.star_map;

import irden.space.proxy.protocol.codec.BinaryCodec;
import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.VlqUnsignedCodec;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class StarNetMapCodec<A, B> implements BinaryCodec<Map<A, B>> {
    private static final int DELTA_END = 0;
    private static final int DELTA_FULL_RELOAD = 1;
    private static final int DELTA_SINGLE_CHANGE = 2;

    private static final int CHANGE_SET = 0;
    private static final int CHANGE_REMOVE = 1;
    private static final int CHANGE_CLEAR = 2;

    private final BinaryCodec<A> keyCodec;
    private final BinaryCodec<B> valueCodec;

    public StarNetMapCodec(BinaryCodec<A> keyCodec, BinaryCodec<B> valueCodec) {
        this.keyCodec = Objects.requireNonNull(keyCodec, "keyCodec");
        this.valueCodec = Objects.requireNonNull(valueCodec, "valueCodec");
    }

    /**
     * Reads a full map payload as written by OpenStarbound netStore():
     *
     * <pre>
     * count: vlqU
     * repeated count times:
     *   changeType: u8
     *   change payload
     * </pre>
     *
     * Even though a normal full snapshot usually contains only SetChange,
     * OpenStarbound technically reads generic changes here, so we support all of them.
     */
    @Override
    public Map<A, B> read(BinaryReader reader) {
        int size = VlqUnsignedCodec.INSTANCE.read(reader);
        Map<A, B> map = LinkedHashMap.newLinkedHashMap(Math.max(size, 0));

        for (int i = 0; i < size; i++) {
            applyOneChange(reader, map);
        }

        return map;
    }

    /**
     * Convenience method:
     * reads a delta stream against an empty base map.
     *
     * Safe when:
     * - the sender emits full reloads, or
     * - you know there are only additive/overwrite changes.
     *
     * For true incremental synchronization use {@link #readDelta(BinaryReader, Map)}.
     */
    public Map<A, B> readDelta(BinaryReader reader) {
        return readDelta(reader, Map.of());
    }

    /**
     * Reads a delta stream and applies it to a copy of {@code baseMap}.
     *
     * Delta format:
     * - 0 = end
     * - 1 = full reload, followed by {@link #read(BinaryReader)}
     * - 2 = single change, followed by one change record
     */
    public Map<A, B> readDelta(BinaryReader reader, Map<A, B> baseMap) {
        Map<A, B> map = new LinkedHashMap<>(baseMap);

        while (true) {
            int code = VlqUnsignedCodec.INSTANCE.read(reader);

            if (code == DELTA_END) {
                break;
            } else if (code == DELTA_FULL_RELOAD) {
                map = read(reader);
            } else if (code == DELTA_SINGLE_CHANGE) {
                applyOneChange(reader, map);
            } else {
                throw new IllegalStateException("Unknown NetElementHashMap delta code: " + code);
            }
        }

        return map;
    }

    /**
     * Mutates an existing target map by applying the incoming delta to it.
     * This is the closest semantic equivalent to OpenStarbound readNetDelta().
     */
    public void readDeltaInto(BinaryReader reader, Map<A, B> target) {
        while (true) {
            int code = VlqUnsignedCodec.INSTANCE.read(reader);

            if (code == DELTA_END) {
                return;
            } else if (code == DELTA_FULL_RELOAD) {
                Map<A, B> reloaded = read(reader);
                target.clear();
                target.putAll(reloaded);
            } else if (code == DELTA_SINGLE_CHANGE) {
                applyOneChange(reader, target);
            } else {
                throw new IllegalStateException("Unknown NetElementHashMap delta code: " + code);
            }
        }
    }

    /**
     * Writes a full snapshot payload compatible with OpenStarbound netStore():
     *
     * <pre>
     * count: vlqU
     * repeated count times:
     *   0 (SetChange): u8
     *   key
     *   value
     * </pre>
     */
    @Override
    public void write(BinaryWriter writer, Map<A, B> value) {
        VlqUnsignedCodec.INSTANCE.write(writer, value.size());

        for (Map.Entry<A, B> entry : value.entrySet()) {
            writer.writeByte(CHANGE_SET);
            keyCodec.write(writer, entry.getKey());
            valueCodec.write(writer, entry.getValue());
        }
    }

    /**
     * Writes an empty delta stream:
     *
     * <pre>
     * 0
     * </pre>
     *
     * Useful if outer code has already decided to emit the field and now needs
     * a syntactically valid empty body.
     */
    public void writeEmptyDelta(BinaryWriter writer) {
        VlqUnsignedCodec.INSTANCE.write(writer, DELTA_END);
    }

    /**
     * Writes a full-reload delta:
     *
     * <pre>
     * 1
     * full snapshot payload
     * 0
     * </pre>
     */
    public void writeFullReloadDelta(BinaryWriter writer, Map<A, B> value) {
        VlqUnsignedCodec.INSTANCE.write(writer, DELTA_FULL_RELOAD);
        write(writer, value);
        VlqUnsignedCodec.INSTANCE.write(writer, DELTA_END);
    }

    /**
     * Writes an incremental delta from oldMap to newMap.
     *
     * Returns true if anything was written, false if maps are equal and nothing
     * was emitted at all.
     *
     * Protocol:
     * - each individual change is encoded as:
     *     2 + one change record
     * - after the last change an ending 0 is written
     *
     * This mirrors OpenStarbound NetElementMapWrapper::writeNetDelta().
     */
    public boolean writeDelta(BinaryWriter writer, Map<A, B> oldMap, Map<A, B> newMap) {
        boolean deltaWritten = false;

        if (Objects.equals(oldMap, newMap)) {
            return false;
        }

        // Small optimization: if map became empty, encode as a single ClearChange.
        if (!oldMap.isEmpty() && newMap.isEmpty()) {
            VlqUnsignedCodec.INSTANCE.write(writer, DELTA_SINGLE_CHANGE);
            writer.writeByte(CHANGE_CLEAR);

            VlqUnsignedCodec.INSTANCE.write(writer, DELTA_END);
            return true;
        }

        // Set / overwrite changed or new keys.
        for (Map.Entry<A, B> entry : newMap.entrySet()) {
            A key = entry.getKey();
            B newValue = entry.getValue();

            boolean hadOld = oldMap.containsKey(key);
            B oldValue = oldMap.get(key);

            if (!hadOld || !Objects.equals(oldValue, newValue)) {
                VlqUnsignedCodec.INSTANCE.write(writer, DELTA_SINGLE_CHANGE);
                writer.writeByte(CHANGE_SET);
                keyCodec.write(writer, key);
                valueCodec.write(writer, newValue);
                deltaWritten = true;
            }
        }

        // Remove missing keys.
        for (A key : oldMap.keySet()) {
            if (!newMap.containsKey(key)) {
                VlqUnsignedCodec.INSTANCE.write(writer, DELTA_SINGLE_CHANGE);
                writer.writeByte(CHANGE_REMOVE);
                keyCodec.write(writer, key);
                deltaWritten = true;
            }
        }

        if (deltaWritten) {
            VlqUnsignedCodec.INSTANCE.write(writer, DELTA_END);
        }

        return deltaWritten;
    }

    private void applyOneChange(BinaryReader reader, Map<A, B> map) {
        int changeCode = reader.readUnsignedByte();

        if (changeCode == CHANGE_SET) {
            A key = keyCodec.read(reader);
            B value = valueCodec.read(reader);
            map.put(key, value);
        } else if (changeCode == CHANGE_REMOVE) {
            A key = keyCodec.read(reader);
            map.remove(key);
        } else if (changeCode == CHANGE_CLEAR) {
            map.clear();
        } else {
            throw new IllegalStateException("Unknown NetElementHashMap change code: " + changeCode);
        }
    }
}
