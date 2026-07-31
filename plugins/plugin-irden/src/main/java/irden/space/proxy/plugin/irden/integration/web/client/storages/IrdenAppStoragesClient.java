package irden.space.proxy.plugin.irden.integration.web.client.storages;

import irden.space.proxy.plugin.irden.integration.web.client.constants.IrdenAppRoutes;
import irden.space.proxy.plugin.irden.integration.web.client.exceptions.IrdenAppClientException;
import irden.space.proxy.plugin.irden.integration.web.client.storages.dto.StorageAttributes;
import irden.space.proxy.plugin.irden.integration.web.client.storages.dto.StorageId;
import irden.space.proxy.plugin.irden.integration.web.client.storages.dto.StorageItem;
import irden.space.proxy.plugin.irden.integration.web.client.storages.dto.StoragesResponse;
import irden.space.proxy.plugin.irden.integration.web.dto.player_app_id.PlayerAppIdParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;

public interface IrdenAppStoragesClient {

    @GetExchange(
            url = IrdenAppRoutes.Storage.FIND + PlayerAppIdParam.PATH
    )
    StoragesResponse<StorageAttributes> findStorageByPlayerAppId(@PathVariable(PlayerAppIdParam.NAME) PlayerAppIdParam playerAppIdParam) throws IrdenAppClientException;


    @GetExchange(
            url = IrdenAppRoutes.Storage.ROOT + StorageId.PATH
    )
    StoragesResponse<StorageItem> getStorageItemsByStorageId(@PathVariable(StorageId.NAME) StorageId storageId) throws IrdenAppClientException;

}