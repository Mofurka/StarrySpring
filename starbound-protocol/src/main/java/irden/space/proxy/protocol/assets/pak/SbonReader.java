package irden.space.proxy.protocol.assets.pak;

import java.io.DataInput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class SbonReader {

    Object readDynamicValue(DataInput input) throws IOException {
        int type = input.readUnsignedByte();
        return switch (type) {
            case 1 -> null;
            case 2 -> input.readDouble();
            case 3 -> input.readUnsignedByte() != 0;
            case 4 -> readSignedVarInt(input);
            case 5 -> readString(input);
            case 6 -> readList(input);
            case 7 -> readMap(input);
            default -> throw new IllegalStateException("Unknown SBON type: 0x" + Integer.toHexString(type));
        };
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> readMapValue(DataInput input) throws IOException {
        Object value = readMap(input);
        return (Map<String, Object>) value;
    }

    private List<Object> readList(DataInput input) throws IOException {
        long size = readVarInt(input);
        if (size > Integer.MAX_VALUE) {
            throw new IllegalStateException("SBON list is too large: " + size);
        }
        List<Object> values = new ArrayList<>((int) size);
        for (int i = 0; i < size; i++) {
            values.add(readDynamicValue(input));
        }
        return values;
    }

    private Map<String, Object> readMap(DataInput input) throws IOException {
        long size = readVarInt(input);
        if (size > Integer.MAX_VALUE) {
            throw new IllegalStateException("SBON map is too large: " + size);
        }
        Map<String, Object> values = new LinkedHashMap<>((int) size);
        for (int i = 0; i < size; i++) {
            values.put(readString(input), readDynamicValue(input));
        }
        return values;
    }

    long readVarInt(DataInput input) throws IOException {
        long value = 0;
        while (true) {
            int next = input.readUnsignedByte();
            value = (value << 7) | (next & 0x7F);
            if ((next & 0x80) == 0) {
                return value;
            }
        }
    }

    private long readSignedVarInt(DataInput input) throws IOException {
        long raw = readVarInt(input);
        if ((raw & 1L) == 0L) {
            return raw >>> 1;
        }
        return -((raw >>> 1) + 1);
    }

    String readString(DataInput input) throws IOException {
        long size = readVarInt(input);
        if (size > Integer.MAX_VALUE) {
            throw new IllegalStateException("SBON string is too large: " + size);
        }
        byte[] data = new byte[(int) size];
        input.readFully(data);
        return new String(data, StandardCharsets.UTF_8);
    }
}
