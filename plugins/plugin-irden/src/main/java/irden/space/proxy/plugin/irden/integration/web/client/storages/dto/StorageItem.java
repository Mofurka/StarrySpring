package irden.space.proxy.plugin.irden.integration.web.client.storages.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public record StorageItem(
        @JsonAlias("storage_name")
        @JsonProperty("storage_name")
        String storageName,
        @JsonAlias("item_name")
        @JsonProperty("item_name")
        String itemName,
        @JsonAlias("category_name")
        @JsonProperty("category_name")
        String categoryName,
        @JsonAlias("item_description")
        @JsonProperty("item_description")
        String itemDescription,
        @JsonAlias("item_amount")
        @JsonProperty("item_amount")
        int itemAmount,
        @JsonAlias("item_id")
        @JsonProperty("item_id")
        int itemId,
        @JsonAlias("item_icon")
        @JsonProperty("item_icon")
        String itemIcon,
        @JsonAlias("item_cost")
        @JsonProperty("item_cost")
        Integer itemCost,
        @JsonAlias("item_public")
        @JsonProperty("item_public")
        boolean isItemPublic
) {
}
