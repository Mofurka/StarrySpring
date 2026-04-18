package irden.space.proxy.protocol.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.experimental.UtilityClass;

@UtilityClass
public class JsonUtils {

    public static JsonNode merge(JsonNode base, JsonNode update) {
        if (base == null) return update;
        if (update == null) return base;

        if (!base.isObject() || !update.isObject()) {
            return update;
        }

        ObjectNode result = base.deepCopy();

        update.properties().forEach(entry -> {
            String key = entry.getKey();
            JsonNode value = entry.getValue();

            if (result.has(key) && result.get(key).isObject() && value.isObject()) {
                result.set(key, merge(result.get(key), value));
            } else {
                result.set(key, value);
            }
        });

        return result;
    }

}