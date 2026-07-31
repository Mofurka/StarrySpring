package irden.space.proxy.plugin.irden.integration.web.client.storages.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record StorageAttributes(
        @JsonAlias("warehouse_id")
        int storageId,
        @JsonAlias("name")
        String storageName,
        @JsonAlias("is_public")
        boolean isPublic
) {
}