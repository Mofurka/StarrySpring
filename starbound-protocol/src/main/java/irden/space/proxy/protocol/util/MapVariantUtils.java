package irden.space.proxy.protocol.util;

import irden.space.proxy.protocol.codec.variant.*;
import lombok.experimental.UtilityClass;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.*;

import java.util.LinkedHashMap;
import java.util.Map;

@UtilityClass
public final class MapVariantUtils {
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();


    public static VariantValue get(MapVariantValue map, String... deepKeys) {
        if (deepKeys == null || deepKeys.length == 0) {
            return map;
        }

        VariantValue current = map;

        for (String key : deepKeys) {
            if (!(current instanceof MapVariantValue(Map<String, VariantValue> mapValue))) {
                return null;
            }

            current = mapValue.get(key);

            if (current == null) {
                return null;
            }
        }

        return current;
    }


    public static String getString(MapVariantValue map, String... deepKeys) {
        VariantValue value = get(map, deepKeys);
        if (value instanceof StringVariantValue(String stringValue)) {
            return stringValue;
        }
        return null;
    }


    public static Integer getInt(MapVariantValue map, String... deepKeys) {
        Long value = getLong(map, deepKeys);
        if (value == null || value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            return null;
        }
        return value.intValue();
    }


    public static Long getLong(MapVariantValue map, String... deepKeys) {
        VariantValue value = get(map, deepKeys);
        if (value instanceof IntVariantValue(long longValue)) {
            return longValue;
        }
        return null;
    }


    public static Boolean getBoolean(MapVariantValue map, String... deepKeys) {
        VariantValue value = get(map, deepKeys);
        if (value instanceof BooleanVariantValue(boolean booleanValue)) {
            return booleanValue;
        }
        return null;
    }


    public static Double getDouble(MapVariantValue map, String... deepKeys) {
        VariantValue value = get(map, deepKeys);
        if (value instanceof DoubleVariantValue(double doubleValue)) {
            return doubleValue;
        }
        return null;
    }


    public static VariantValue[] getList(MapVariantValue map, String... deepKeys) {
        VariantValue value = get(map, deepKeys);
        if (value instanceof ListVariantValue(VariantValue[] listValue)) {
            return listValue;
        }
        return null;
    }


    public static Map<String, VariantValue> getMap(MapVariantValue map, String... deepKeys) {
        VariantValue value = get(map, deepKeys);
        if (value instanceof MapVariantValue(Map<String, VariantValue> mapValue)) {
            return mapValue;
        }
        return null;
    }


    public static boolean contains(MapVariantValue map, String... deepKeys) {
        return get(map, deepKeys) != null;
    }


    public static JsonNode merge(JsonNode jsonNode, MapVariantValue mapVariant) {
        if (jsonNode == null && mapVariant == null) {
            return NullNode.getInstance();
        }
        if (jsonNode == null) {
            return variantToJsonNode(mapVariant);
        }
        if (mapVariant == null) {
            return jsonNode;
        }

        ObjectNode result;
        if (jsonNode.isObject()) {
            result = jsonNode.deepCopy().asObject();
        } else {
            result = JsonNodeFactory.instance.objectNode();
        }

        for (Map.Entry<String, VariantValue> entry : mapVariant.value().entrySet()) {
            String key = entry.getKey();
            VariantValue variantValue = entry.getValue();

            if (result.has(key) && result.get(key).isObject()
                    && variantValue instanceof MapVariantValue nestedMap) {
                result.set(key, merge(result.get(key), nestedMap));
            } else {
                result.set(key, variantToJsonNode(variantValue));
            }
        }

        return result;
    }


    public static JsonNode variantToJsonNode(VariantValue value) {
        if (value == null) {
            return NullNode.getInstance();
        }

        return switch (value) {
            case NullVariantValue _ -> NullNode.getInstance();
            case BooleanVariantValue(boolean boolValue) -> BooleanNode.valueOf(boolValue);
            case IntVariantValue(long intValue) -> intValue >= Integer.MIN_VALUE && intValue <= Integer.MAX_VALUE
                    ? IntNode.valueOf((int) intValue)
                    : LongNode.valueOf(intValue);
            case DoubleVariantValue(double doubleValue) -> DoubleNode.valueOf(doubleValue);
            case StringVariantValue(String stringValue) -> StringNode.valueOf(stringValue);
            case ListVariantValue(VariantValue[] listValues) -> {
                ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
                for (VariantValue item : listValues) {
                    arrayNode.add(variantToJsonNode(item));
                }
                yield arrayNode;
            }
            case MapVariantValue(Map<String, VariantValue> mapValues) -> {
                ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
                for (Map.Entry<String, VariantValue> entry : mapValues.entrySet()) {
                    objectNode.set(entry.getKey(), variantToJsonNode(entry.getValue()));
                }
                yield objectNode;
            }
        };
    }

    public static VariantValue objectToVariant(Object object) {
        return jsonNodeToVariant(JSON_MAPPER.valueToTree(object));
    }

    public static VariantValue jsonNodeToVariant(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return NullVariantValue.INSTANCE;
        }

        if (node.isBoolean()) {
            return new BooleanVariantValue(node.booleanValue());
        }

        if (node.isInt()) {
            return new IntVariantValue(node.intValue());
        }

        if (node.isLong()) {
            return new IntVariantValue(node.longValue());
        }

        if (node.isFloatingPointNumber()) {
            /*
             * Обработает FloatNode, DoubleNode и DecimalNode.
             */
            return new DoubleVariantValue(node.doubleValue());
        }

        if (node.isString()) {
            return new StringVariantValue(node.stringValue());
        }

        if (node.isArray()) {
            VariantValue[] values = new VariantValue[node.size()];

            int index = 0;
            for (JsonNode element : node.values()) {
                values[index++] = jsonNodeToVariant(element);
            }

            return new ListVariantValue(values);
        }

        if (node.isObject()) {
            Map<String, VariantValue> map = new LinkedHashMap<>();

            node.forEachEntry((name, value) ->
                    map.put(name, jsonNodeToVariant(value))
            );

            return new MapVariantValue(map);
        }

        return NullVariantValue.INSTANCE;
    }
}
