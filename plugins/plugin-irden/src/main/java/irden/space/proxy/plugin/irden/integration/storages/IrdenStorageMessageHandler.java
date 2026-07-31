package irden.space.proxy.plugin.irden.integration.storages;

import irden.space.proxy.plugin.command_handler.entity_message.EntityMessageContext;
import irden.space.proxy.plugin.command_handler.entity_message.EntityMessageHandler;
import irden.space.proxy.plugin.irden.integration.web.client.storages.IrdenAppStoragesClient;
import irden.space.proxy.plugin.irden.integration.web.client.storages.dto.*;
import irden.space.proxy.plugin.irden.integration.web.dto.player_app_id.PlayerAppIdParam;
import irden.space.proxy.plugin.player_manager.api.PlayerManagerApi;
import irden.space.proxy.plugin.player_manager.model.Player;
import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.codec.variant.Variants;
import irden.space.proxy.protocol.util.MapVariantUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class IrdenStorageMessageHandler {
    private final PlayerManagerApi playerManagerApi;
    private final IrdenAppStoragesClient irdenAppStoragesClient;
    private final ObjectMapper jsonMapper;


    @EntityMessageHandler("warehouses:request")
    public VariantValue onWarehousesRequest(EntityMessageContext ctx) {
        Optional<Player> playerBySessionId = playerManagerApi.findPlayerBySessionId(ctx.session().sessionId());
        if (playerBySessionId.isPresent()) {
            Player player = playerBySessionId.get();
            Long applicationId = (Long) player.metadata().get("applicationId");
            StoragesResponse<StorageAttributes> storageByPlayerAppId = irdenAppStoragesClient.findStorageByPlayerAppId(new PlayerAppIdParam(applicationId));
            return MapVariantUtils.objectToVariant(storageByPlayerAppId.data());
        }
        return null;
    }

    @EntityMessageHandler("warehouse:request")
    public VariantValue onWarehouseRequest(EntityMessageContext ctx) {
        Optional<Integer> storageId = Variants.asInt(ctx.arg(0));
        if (storageId.isPresent()) {
            StoragesResponse<StorageItem> storageItemsByStorageId = irdenAppStoragesClient.getStorageItemsByStorageId(new StorageIdParam(storageId.get()));
            return MapVariantUtils.objectToVariant(storageItemsByStorageId.data());
        }
        return Variants.of("ID склада не указан. Пожалуйста обратитесь к администратору");
    }

    @EntityMessageHandler("warehouse:transfer_request")
    public VariantValue onWarehouseTransferRequest(EntityMessageContext ctx) {
        JsonNode jsonNode = MapVariantUtils.variantToJsonNode(ctx.arg(0));
        StorageTransferRequest storageTransferRequest = jsonMapper.readValue(jsonNode.toString(), StorageTransferRequest.class);
        irdenAppStoragesClient.makeTransferRequest(storageTransferRequest);
        return null;
    }
}
