package irden.space.proxy.protocol.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import irden.space.proxy.protocol.codec.variant.*;
import lombok.experimental.UtilityClass;

import java.util.*;

@UtilityClass
public final class MapVariantUtils {

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
        VariantValue value = get(map, deepKeys);
        if (value instanceof IntVariantValue(int intValue)) {
            return intValue;
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


    public static List<VariantValue> getList(MapVariantValue map, String... deepKeys) {
        VariantValue value = get(map, deepKeys);
        if (value instanceof ListVariantValue(List<VariantValue> listValue)) {
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
            result = ((ObjectNode) jsonNode).deepCopy();
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
            case IntVariantValue(int intValue) -> IntNode.valueOf(intValue);
            case DoubleVariantValue(double doubleValue) -> DoubleNode.valueOf(doubleValue);
            case StringVariantValue(String stringValue) -> TextNode.valueOf(stringValue);
            case ListVariantValue(List<VariantValue> listValues) -> {
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

    public static VariantValue jsonNodeToVariant(JsonNode node) {
        if (node == null || node.isNull()) {
            return NullVariantValue.INSTANCE;
        }

        if (node.isBoolean()) {
            return new BooleanVariantValue(node.asBoolean());
        }
        if (node.isInt()) {
            return new IntVariantValue(node.asInt());
        }
        if (node.isDouble() || node.isFloat()) {
            return new DoubleVariantValue(node.asDouble());
        }
        if (node.isLong()) {
            // Конвертируем long в int (может быть потеря данных)
            return new IntVariantValue(node.asInt());
        }
        if (node.isTextual()) {
            return new StringVariantValue(node.asText());
        }
        if (node.isArray()) {
            List<VariantValue> values = new ArrayList<>();
            for (JsonNode element : node) {
                values.add(jsonNodeToVariant(element));
            }
            return new ListVariantValue(values);
        }
        if (node.isObject()) {
            Map<String, VariantValue> map = new LinkedHashMap<>();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                map.put(entry.getKey(), jsonNodeToVariant(entry.getValue()));
            }
            return new MapVariantValue(map);
        }

        return NullVariantValue.INSTANCE;
    }

}
