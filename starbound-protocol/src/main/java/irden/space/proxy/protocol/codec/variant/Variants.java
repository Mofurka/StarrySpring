package irden.space.proxy.protocol.codec.variant;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Фабрики и хелперы для {@link VariantValue} - чтобы не собирать варианты руками.
 *
 * <pre>{@code
 *
 * // вложенная структура:
 * VariantValue payload = Variants.mapBuilder()
 *         .put("name", "Steve")
 *         .put("level", 42)
 *         .put("tags", Variants.listOf("a", "b"))
 *         .build();
 * }</pre>
 */
public final class Variants {

    private Variants() {
    }


    public static VariantValue nul() {
        return NullVariantValue.INSTANCE;
    }

    public static VariantValue of(String value) {
        return value == null ? nul() : new StringVariantValue(value);
    }

    public static VariantValue of(boolean value) {
        return new BooleanVariantValue(value);
    }

    public static VariantValue of(int value) {
        return new IntVariantValue(value);
    }


    public static VariantValue of(long value) {
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "long " + value + " does not fit into an int variant; use of(double) instead");
        }
        return new IntVariantValue((int) value);
    }

    public static VariantValue of(double value) {
        return new DoubleVariantValue(value);
    }


    public static VariantValue of(Object value) {
        return switch (value) {
            case null -> nul();
            case VariantValue variantValue -> variantValue;
            case String string -> new StringVariantValue(string);
            case Boolean bool -> new BooleanVariantValue(bool);
            case Integer number -> new IntVariantValue(number);
            case Short number -> new IntVariantValue(number.intValue());
            case Byte number -> new IntVariantValue(number.intValue());
            case Long number -> of(number.longValue());
            case Double number -> new DoubleVariantValue(number);
            case Float number -> new DoubleVariantValue(number.doubleValue());
            case Number number -> new DoubleVariantValue(number.doubleValue());
            case Map<?, ?> map -> mapOf(map);
            case Collection<?> collection -> list(collection);
            case Object[] array -> listOf(array);
            default -> throw new IllegalArgumentException(
                    "Cannot convert to VariantValue: " + value.getClass().getName());
        };
    }


    public static ListVariantValue list(VariantValue... values) {
        return new ListVariantValue(values == null ? new VariantValue[0] : values.clone());
    }

    public static ListVariantValue listOf(Object... values) {
        return new ListVariantValue(args(values));
    }

    public static ListVariantValue list(Collection<?> values) {
        if (values == null) {
            return new ListVariantValue(new VariantValue[0]);
        }
        VariantValue[] result = new VariantValue[values.size()];
        int index = 0;
        for (Object value : values) {
            result[index++] = of(value);
        }
        return new ListVariantValue(result);
    }


    public static MapVariantValue mapOf(Map<?, ?> values) {
        Map<String, VariantValue> result = new LinkedHashMap<>();
        if (values != null) {
            values.forEach((key, value) -> result.put(String.valueOf(key), of(value)));
        }
        return new MapVariantValue(Collections.unmodifiableMap(result));
    }

    public static MapBuilder mapBuilder() {
        return new MapBuilder();
    }


    public static VariantValue[] args(Object... values) {
        if (values == null) {
            return new VariantValue[0];
        }
        VariantValue[] result = new VariantValue[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = of(values[i]);
        }
        return result;
    }


    public static Optional<String> asString(VariantValue value) {
        return value instanceof StringVariantValue(String string) ? Optional.of(string) : Optional.empty();
    }

    public static Optional<Integer> asInt(VariantValue value) {
        return value instanceof IntVariantValue(int number) ? Optional.of(number) : Optional.empty();
    }

    public static Optional<Double> asDouble(VariantValue value) {
        return value instanceof DoubleVariantValue(double number) ? Optional.of(number) : Optional.empty();
    }

    public static Optional<Boolean> asBoolean(VariantValue value) {
        return value instanceof BooleanVariantValue(boolean bool) ? Optional.of(bool) : Optional.empty();
    }

    public static Optional<Map<String, VariantValue>> asMap(VariantValue value) {
        return value instanceof MapVariantValue(Map<String, VariantValue> map) ? Optional.of(map) : Optional.empty();
    }

    public static Optional<VariantValue[]> asList(VariantValue value) {
        return value instanceof ListVariantValue(VariantValue[] values) ? Optional.of(values) : Optional.empty();
    }

    public static Optional<VariantValue> get(VariantValue value, String key) {
        return asMap(value).map(map -> map.get(key));
    }

    public static final class MapBuilder {

        private final Map<String, VariantValue> values = new LinkedHashMap<>();

        private MapBuilder() {
        }

        public MapBuilder put(String key, Object value) {
            values.put(key, of(value));
            return this;
        }

        public MapVariantValue build() {
            return new MapVariantValue(Collections.unmodifiableMap(new LinkedHashMap<>(values)));
        }
    }
}
