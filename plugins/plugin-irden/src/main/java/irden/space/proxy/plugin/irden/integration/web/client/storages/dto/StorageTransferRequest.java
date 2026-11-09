package irden.space.proxy.plugin.irden.integration.web.client.storages.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record StorageTransferRequest(
        @JsonProperty("source_storage_id")
        Long sourceStorageId,
        @JsonProperty("target_storage_id")
        Long targetStorageId,
        @JsonProperty("request_item_list")
        List<TransferItem> requestItemList,
        @JsonProperty("request_character")
        Long requestApplicationId,
        @JsonProperty("comment")
        String transferDescription,
        @JsonProperty("autoAccept")
        boolean autoAccept,

        @JsonProperty("request_author_uuid")
        String requestAuthorUuid
) {

    public StorageTransferRequest setRequestApplicationId(Long applicationId) {
        return new StorageTransferRequest(
                this.sourceStorageId,
                this.targetStorageId,
                this.requestItemList,
                applicationId,
                this.transferDescription,
                this.autoAccept,
                this.requestAuthorUuid
        );
    }

    public record TransferItem(
            @JsonProperty("item")
            Long itemId,
            @JsonProperty("amount")
            Long amount
    ) {
    }
}
