package irden.space.proxy.protocol.assets.item;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;


@Data
public class ActiveItem {

    private final String itemName;

    private final JsonNode data;

    private final String itemDirectory;

    public JsonNode get(String fieldName) {
        return data.get(fieldName);
    }

    public boolean has(String fieldName) {
        return data.has(fieldName);
    }

}

