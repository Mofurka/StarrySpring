package irden.space.proxy.plugin.irden.integration.web.client.storages.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record StorageItem(
        @JsonAlias("storage_name")
        String storageName,
        @JsonAlias("item_name")
        String itemName,
        @JsonAlias("category_name")
        String categoryName,
        @JsonAlias("item_description")
        String itemDescription,
        @JsonAlias("item_amount")
        int itemAmount,
        @JsonAlias("item_id")
        int itemId,
        @JsonAlias("item_icon")
        String itemIcon,
        @JsonAlias("item_cost")
        Integer itemCost,
        @JsonAlias("item_public")
        boolean isItemPublic
) {
}
