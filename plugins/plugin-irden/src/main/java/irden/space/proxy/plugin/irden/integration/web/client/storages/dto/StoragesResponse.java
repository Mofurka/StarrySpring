package irden.space.proxy.plugin.irden.integration.web.client.storages.dto;

import java.util.List;

public record StoragesResponse<T>(
        List<T> data
) {
}
