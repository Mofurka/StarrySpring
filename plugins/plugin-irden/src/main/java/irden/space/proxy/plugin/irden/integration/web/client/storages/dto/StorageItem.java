package irden.space.proxy.plugin.irden.integration.web.client.storages.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StorageItem(
        @JsonProperty("storage_name")
        String storageName,
        @JsonProperty("item_name")
        String itemName,
        @JsonProperty("category_name")
        String categoryName,
        @JsonProperty("item_description")
        String itemDescription,
        @JsonProperty("item_amount")
        Integer itemAmount,
        @JsonProperty("item_id")
        int itemId,
        @JsonProperty("item_icon")
        String itemIcon,
        @JsonProperty("item_cost")
        Integer itemCost,
        @JsonProperty("item_public")
        boolean isItemPublic
) {
}
