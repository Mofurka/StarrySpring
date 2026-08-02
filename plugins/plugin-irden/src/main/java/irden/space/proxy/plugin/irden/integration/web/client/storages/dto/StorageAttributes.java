package irden.space.proxy.plugin.irden.integration.web.client.storages.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StorageAttributes(
        @JsonProperty("warehouse_id")
        int storageId,
        @JsonProperty("name")
        String storageName,
        @JsonProperty("is_public")
        boolean isPublic
) {
}