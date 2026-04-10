package irden.space.proxy.protocol.codec;

import irden.space.proxy.protocol.codec.variant.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class VariantCodec {

    private VariantCodec() {
    }

    public static VariantValue read(BinaryReader reader) {
        int type = reader.readUnsignedByte();

        return switch (type) {
            case 1 -> NullVariantValue.INSTANCE;
            case 2 -> new DoubleVariantValue(reader.readDouble64BE());
            case 3 -> new BooleanVariantValue(reader.readBoolean());
            case 4 -> new IntVariantValue(SignedVlqCodec.read(reader));
            case 5 -> new StringVariantValue(StarStringCodec.read(reader));
            case 6 -> new ListVariantValue(readList(reader));
            case 7 -> new MapVariantValue(readMap(reader));
            default -> throw new IllegalStateException("Unknown variant type: " + type);
        };
    }

    public static void write(BinaryWriter writer, VariantValue value) {
        switch (value) {
            case NullVariantValue nullVariantValue -> writer.writeByte(1);
            case DoubleVariantValue(double value1) -> {
                writer.writeByte(2);
                writer.writeDouble64BE(value1);
            }
            case BooleanVariantValue(boolean value1) -> {
                writer.writeByte(3);
                writer.writeBoolean(value1);
            }
            case IntVariantValue(int value1) -> {
                writer.writeByte(4);
                SignedVlqCodec.write(writer, value1);
            }
            case StringVariantValue(String value1) -> {
                writer.writeByte(5);
                StarStringCodec.write(writer, value1);
            }
            case ListVariantValue(List<VariantValue> values) -> {
                writer.writeByte(6);
                writeList(writer, values);
            }
            case MapVariantValue(Map<String, VariantValue> value1) -> {
                writer.writeByte(7);
                writeMap(writer, value1);
            }
            case null, default -> throw new IllegalStateException("Unsupported variant value: " + value);
        }
    }

    public static List<VariantValue> readList(BinaryReader reader) {
        int size = VlqCodec.read(reader);
        List<VariantValue> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(read(reader));
        }
        return result;
    }

    public static void writeList(BinaryWriter writer, List<VariantValue> values) {
        VlqCodec.write(writer, values.size());
        for (VariantValue value : values) {
            write(writer, value);
        }
    }

    public static Map<String, VariantValue> readMap(BinaryReader reader) {
        int size = VlqCodec.read(reader);
        Map<String, VariantValue> result = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            String key = StarStringCodec.read(reader);
            VariantValue value = read(reader);
            result.put(key, value);
        }
        return result;
    }

    public static void writeMap(BinaryWriter writer, Map<String, VariantValue> values) {
        VlqCodec.write(writer, values.size());
        for (Map.Entry<String, VariantValue> entry : values.entrySet()) {
            StarStringCodec.write(writer, entry.getKey());
            write(writer, entry.getValue());
        }
    }
}
