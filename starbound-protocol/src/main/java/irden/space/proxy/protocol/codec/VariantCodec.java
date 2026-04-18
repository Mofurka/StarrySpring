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
            case 4 -> new IntVariantValue(VlqCodec.INSTANCE.read(reader));
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
                VlqCodec.INSTANCE.write(writer, value1);
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
        int size = VlqUCodec.INSTANCE.read(reader);
        List<VariantValue> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(read(reader));
        }
        return result;
    }

    public void writeList(BinaryWriter writer, List<VariantValue> values) {
        VlqUCodec.INSTANCE.write(writer, values.size());
        for (VariantValue value : values) {
            write(writer, value);
        }
    }

    public Map<String, VariantValue> readMap(BinaryReader reader) {
        int size = VlqUCodec.INSTANCE.read(reader);
        Map<String, VariantValue> result = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            String key = StarStringCodec.INSTANCE.read(reader);
            VariantValue value = read(reader);
            result.put(key, value);
        }
        return result;
    }

    public void writeMap(BinaryWriter writer, Map<String, VariantValue> values) {
        VlqUCodec.INSTANCE.write(writer, values.size());
        for (Map.Entry<String, VariantValue> entry : values.entrySet()) {
            StarStringCodec.INSTANCE.write(writer, entry.getKey());
            write(writer, entry.getValue());
        }
    }

    /**
     * Converts a VariantValue to a JSON-like string representation.
     */
    public String toString(VariantValue value) {
        return switch (value) {
            case NullVariantValue _ -> "null";
            case DoubleVariantValue(double v) -> String.valueOf(v);
            case BooleanVariantValue(boolean v) -> String.valueOf(v);
            case IntVariantValue(int v) -> String.valueOf(v);
            case StringVariantValue(String v) -> "\"" + escapeString(v) + "\"";
            case ListVariantValue(List<VariantValue> values) -> {
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < values.size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(toString(values.get(i)));
                }
                sb.append("]");
                yield sb.toString();
            }
            case MapVariantValue(Map<String, VariantValue> map) -> {
                StringBuilder sb = new StringBuilder("{");
                int i = 0;
                for (Map.Entry<String, VariantValue> entry : map.entrySet()) {
                    if (i > 0) sb.append(", ");
                    sb.append("\"").append(escapeString(entry.getKey())).append("\": ");
                    sb.append(toString(entry.getValue()));
                    i++;
                }
                sb.append("}");
                yield sb.toString();
            }
            case null -> throw new IllegalStateException("Unsupported variant value: " + value);
        };
    }

    /**
     * Parses a JSON-like string into a VariantValue.
     */
    public VariantValue fromString(String str) {
        return parseValue(new StringParser(str.trim()));
    }

    private VariantValue parseValue(StringParser parser) {
        parser.skipWhitespace();
        char c = parser.peek();

        return switch (c) {
            case 'n' -> {
                parser.expect("null");
                yield NullVariantValue.INSTANCE;
            }
            case 't' -> {
                parser.expect("true");
                yield new BooleanVariantValue(true);
            }
            case 'f' -> {
                parser.expect("false");
                yield new BooleanVariantValue(false);
            }
            case '"' -> new StringVariantValue(parseString(parser));
            case '[' -> new ListVariantValue(parseList(parser));
            case '{' -> new MapVariantValue(parseMap(parser));
            default -> {
                if (c == '-' || Character.isDigit(c)) {
                    yield parseNumber(parser);
                }
                throw new IllegalStateException("Unexpected character: " + c);
            }
        };
    }

    private String parseString(StringParser parser) {
        parser.expect('"');
        StringBuilder sb = new StringBuilder();
        while (parser.hasMore() && parser.peek() != '"') {
            char c = parser.next();
            if (c == '\\') {
                char escaped = parser.next();
                sb.append(switch (escaped) {
                    case 'n' -> '\n';
                    case 't' -> '\t';
                    case 'r' -> '\r';
                    case '\\' -> '\\';
                    case '"' -> '"';
                    default -> escaped;
                });
            } else {
                sb.append(c);
            }
        }
        parser.expect('"');
        return sb.toString();
    }

    private List<VariantValue> parseList(StringParser parser) {
        parser.expect('[');
        List<VariantValue> list = new ArrayList<>();
        parser.skipWhitespace();
        if (parser.peek() == ']') {
            parser.next();
            return list;
        }
        while (true) {
            list.add(parseValue(parser));
            parser.skipWhitespace();
            char c = parser.next();
            if (c == ']') break;
            if (c != ',') throw new IllegalStateException("Expected ',' or ']', got: " + c);
        }
        return list;
    }

    private Map<String, VariantValue> parseMap(StringParser parser) {
        parser.expect('{');
        Map<String, VariantValue> map = new LinkedHashMap<>();
        parser.skipWhitespace();
        if (parser.peek() == '}') {
            parser.next();
            return map;
        }
        while (true) {
            parser.skipWhitespace();
            String key = parseString(parser);
            parser.skipWhitespace();
            parser.expect(':');
            VariantValue value = parseValue(parser);
            map.put(key, value);
            parser.skipWhitespace();
            char c = parser.next();
            if (c == '}') break;
            if (c != ',') throw new IllegalStateException("Expected ',' or '}', got: " + c);
        }
        return map;
    }

    private VariantValue parseNumber(StringParser parser) {
        StringBuilder sb = new StringBuilder();
        boolean isDouble = false;
        while (parser.hasMore()) {
            char c = parser.peek();
            if (c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E' || Character.isDigit(c)) {
                if (c == '.' || c == 'e' || c == 'E') isDouble = true;
                sb.append(parser.next());
            } else {
                break;
            }
        }
        String numStr = sb.toString();
        if (isDouble) {
            return new DoubleVariantValue(Double.parseDouble(numStr));
        } else {
            return new IntVariantValue(Integer.parseInt(numStr));
        }
    }

    private String escapeString(String str) {
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\t", "\\t")
                .replace("\r", "\\r");
    }

    private static class StringParser {
        private final String str;
        private int pos = 0;

        StringParser(String str) {
            this.str = str;
        }

        boolean hasMore() {
            return pos < str.length();
        }

        char peek() {
            if (!hasMore()) throw new IllegalStateException("Unexpected end of input");
            return str.charAt(pos);
        }

        char next() {
            char c = peek();
            pos++;
            return c;
        }

        void expect(char expected) {
            char c = next();
            if (c != expected) {
                throw new IllegalStateException("Expected '" + expected + "', got: " + c);
            }
        }

        void expect(String expected) {
            for (char c : expected.toCharArray()) {
                expect(c);
            }
        }

        void skipWhitespace() {
            while (hasMore() && Character.isWhitespace(peek())) {
                pos++;
            }
        }
    }

}
