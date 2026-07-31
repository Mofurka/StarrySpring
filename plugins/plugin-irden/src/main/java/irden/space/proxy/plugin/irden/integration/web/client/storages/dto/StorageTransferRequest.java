package irden.space.proxy.plugin.irden.integration.web.client.storages.dto;

import java.util.List;

public record StorageTransferRequest(
        Long sourceStorageId,
        Long targetStorageId,
        List<TransferItem> requestItemList,
        Long requestCharacterId,
        String transferDescription,
        boolean autoAccept
        ) {
    public record TransferItem(
            Long itemId,
            Long amount
    ) {
    }
}
