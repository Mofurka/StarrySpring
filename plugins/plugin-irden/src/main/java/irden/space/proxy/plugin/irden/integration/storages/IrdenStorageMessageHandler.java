package irden.space.proxy.plugin.irden.integration.storages;

import irden.space.proxy.plugin.command_handler.entity_message.EntityMessageContext;
import irden.space.proxy.plugin.command_handler.entity_message.EntityMessageHandler;
import irden.space.proxy.plugin.irden.integration.permissions.SitePermissions;
import irden.space.proxy.plugin.irden.integration.web.client.storages.IrdenAppStoragesClient;
import irden.space.proxy.plugin.irden.integration.web.client.storages.dto.*;
import irden.space.proxy.plugin.irden.integration.web.dto.player_discord_id.PlayerDiscordIdParam;
import irden.space.proxy.plugin.player_manager.api.PlayerManagerApi;
import irden.space.proxy.plugin.player_manager.model.Player;
import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.codec.variant.Variants;
import irden.space.proxy.protocol.util.VariantObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class IrdenStorageMessageHandler {
    private final PlayerManagerApi playerManagerApi;
    private final IrdenAppStoragesClient irdenAppStoragesClient;
    private final VariantObjectMapper variantObjectMapper;


    @EntityMessageHandler("warehouses:request")
    public VariantValue onWarehousesRequest(EntityMessageContext ctx) {
        Optional<Player> playerBySessionId = playerManagerApi.findPlayerBySessionId(ctx.session().sessionId());
        if (playerBySessionId.isPresent()) {
            Player player = playerBySessionId.get();
            Long applicationId = (Long) player.metadata().get("discordId");
            StoragesResponse<StorageAttributes> storageByPlayerAppId = irdenAppStoragesClient.findStorageByPlayerDiscordId(new PlayerDiscordIdParam(applicationId));
            return variantObjectMapper.toVariant(storageByPlayerAppId.data());
        }
        return null;
    }

    @EntityMessageHandler("warehouse:request")
    public VariantValue onWarehouseRequest(EntityMessageContext ctx) {
        Optional<Integer> storageId = Variants.asInt(ctx.arg(0));
        if (storageId.isPresent()) {
            StoragesResponse<StorageItem> storageItemsByStorageId = irdenAppStoragesClient.getStorageItemsByStorageId(new StorageIdParam(storageId.get()));
            return variantObjectMapper.toVariant(storageItemsByStorageId.data());
        }
        return Variants.of("ID склада не указан. Пожалуйста обратитесь к администратору");
    }

    @EntityMessageHandler("warehouse:transfer_request")
    public VariantValue onWarehouseTransferRequest(EntityMessageContext ctx) {
        StorageTransferRequest storageTransferRequest = variantObjectMapper.fromVariant(ctx.arg(0), StorageTransferRequest.class);
        if (storageTransferRequest.autoAccept()) {
            boolean has = ctx.session().permissions().has(SitePermissions.IRDEN_STORAGE_AUTO_ACCEPT.permission());
            if (!has)
                throw new IllegalStateException("Вам не доступно автопринятие запроса. ИНЦИДЕНТ БУДЕТ СООБЩЁН АДМИНИСТРАТОРУ!!!!!!!!!!!!!!!");
        }
        Optional<Player> playerBySessionId = playerManagerApi.findPlayerBySessionId(ctx.session().sessionId());
        if (playerBySessionId.isPresent()) {
            Player player = playerBySessionId.get();
            Long applicationId = (Long) player.metadata().get("applicationId");
            irdenAppStoragesClient.makeTransferRequest(storageTransferRequest.setRequestApplicationId(applicationId));
        }
        return null;
    }
}
