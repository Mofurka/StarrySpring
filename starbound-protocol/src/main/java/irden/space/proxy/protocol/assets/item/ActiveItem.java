package irden.space.proxy.protocol.assets.item;

import tools.jackson.databind.JsonNode;


public record ActiveItem(String itemName, JsonNode data, String itemDirectory) {

    public JsonNode get(String fieldName) {
        return data.get(fieldName);
    }

    public boolean has(String fieldName) {
        return data.has(fieldName);
    }

}

