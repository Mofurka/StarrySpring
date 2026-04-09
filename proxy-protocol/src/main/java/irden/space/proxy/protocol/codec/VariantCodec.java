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
            case NullVariantValue ignored -> writer.writeByte(1);
            case DoubleVariantValue(double v) -> {
                writer.writeByte(2);
                writer.writeDouble64BE(v);
            }
            case BooleanVariantValue(boolean v) -> {
                writer.writeByte(3);
                writer.writeBoolean(v);
            }
            case IntVariantValue(int v) -> {
                writer.writeByte(4);
                SignedVlqCodec.write(writer, v);
            }
            case StringVariantValue(String v) -> {
                writer.writeByte(5);
                StarStringCodec.write(writer, v);
            }
            case ListVariantValue(List<VariantValue> v) -> {
                writer.writeByte(6);
                writeList(writer, v);
            }
            case MapVariantValue(Map<String, VariantValue> v) -> {
                writer.writeByte(7);
                writeMap(writer, v);
            }
            default -> throw new IllegalStateException("Unknown variant value type: " + value.getClass().getName());
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
        Map<String, VariantValue> result = LinkedHashMap.newLinkedHashMap(size);
        for (int i = 0; i < size; i++) {
            String key = StarStringCodec.read(reader);
            VariantValue value = read(reader);
            result.put(key, value);
        }
        return result;
    }

    public static void writeMap(BinaryWriter writer, Map<String, VariantValue> map) {
        VlqCodec.write(writer, map.size());
        for (Map.Entry<String, VariantValue> entry : map.entrySet()) {
            StarStringCodec.write(writer, entry.getKey());
            write(writer, entry.getValue());
        }
    }

}
