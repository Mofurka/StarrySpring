package irden.space.proxy.plugin.irden.integration.web.client.storages.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public record StorageAttributes(
        @JsonAlias("warehouse_id")
        @JsonProperty("warehouse_id")
        int storageId,
        @JsonAlias("name")
        @JsonProperty("name")
        String storageName,
        @JsonAlias("is_public")
        @JsonProperty("is_public")
        boolean isPublic
) {
}