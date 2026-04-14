package irden.space.proxy.protocol.codec;

import irden.space.proxy.protocol.codec.variant.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public enum VariantCodec implements BinaryCodec<VariantValue> {
    INSTANCE;

    @Override
    public VariantValue read(BinaryReader reader) {
        int type = reader.readUnsignedByte();

        return switch (type) {
            case 1 -> NullVariantValue.INSTANCE;
            case 2 -> new DoubleVariantValue(reader.readDouble64BE());
            case 3 -> new BooleanVariantValue(reader.readBoolean());
            case 4 -> new IntVariantValue(SignedVlqCodec.INSTANCE.read(reader));
            case 5 -> new StringVariantValue(StarStringCodec.INSTANCE.read(reader));
            case 6 -> new ListVariantValue(readList(reader)); // tuple
            case 7 -> new MapVariantValue(readMap(reader));
            default -> throw new IllegalStateException("Unknown variant type: " + type);
        };
    }

    @Override
    public void write(BinaryWriter writer, VariantValue value) {
        switch (value) {
            case NullVariantValue _ -> writer.writeByte(1);
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
                SignedVlqCodec.INSTANCE.write(writer, value1);
            }
            case StringVariantValue(String value1) -> {
                writer.writeByte(5);
                StarStringCodec.INSTANCE.write(writer, value1);
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

    public List<VariantValue> readList(BinaryReader reader) {
        int size = VlqCodec.INSTANCE.read(reader);
        List<VariantValue> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(read(reader));
        }
        return result;
    }

    public void writeList(BinaryWriter writer, List<VariantValue> values) {
        VlqCodec.INSTANCE.write(writer, values.size());
        for (VariantValue value : values) {
            write(writer, value);
        }
    }

    public Map<String, VariantValue> readMap(BinaryReader reader) {
        int size = VlqCodec.INSTANCE.read(reader);
        Map<String, VariantValue> result = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            String key = StarStringCodec.INSTANCE.read(reader);
            VariantValue value = read(reader);
            result.put(key, value);
        }
        return result;
    }

    public void writeMap(BinaryWriter writer, Map<String, VariantValue> values) {
        VlqCodec.INSTANCE.write(writer, values.size());
        for (Map.Entry<String, VariantValue> entry : values.entrySet()) {
            StarStringCodec.INSTANCE.write(writer, entry.getKey());
            write(writer, entry.getValue());
        }
    }
}
