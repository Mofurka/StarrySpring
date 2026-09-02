package irden.space.proxy.plugin.irden.integration.web.client.storages.dto;

import org.jspecify.annotations.NonNull;

public record StorageIdParam(int storageId) {
    public static final String NAME = "storageId";
    public static final String PATH = "/{" + NAME + "}";

    @Override
    public @NonNull String toString() {
        return Integer.toString(this.storageId);
    }
}
