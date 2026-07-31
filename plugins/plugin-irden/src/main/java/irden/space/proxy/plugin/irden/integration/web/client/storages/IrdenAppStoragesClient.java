package irden.space.proxy.plugin.irden.integration.web.client.storages;

import irden.space.proxy.plugin.irden.integration.web.client.constants.IrdenAppRoutes;
import irden.space.proxy.plugin.irden.integration.web.client.exceptions.IrdenAppClientException;
import irden.space.proxy.plugin.irden.integration.web.client.storages.dto.*;
import irden.space.proxy.plugin.irden.integration.web.dto.player_app_id.PlayerAppIdParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;

public interface IrdenAppStoragesClient {

    @GetExchange(
            url = IrdenAppRoutes.Storage.FIND + PlayerAppIdParam.PATH
    )
    StoragesResponse<StorageAttributes> findStorageByPlayerAppId(@PathVariable(PlayerAppIdParam.NAME) PlayerAppIdParam playerAppIdParam) throws IrdenAppClientException;


    @GetExchange(
            url = IrdenAppRoutes.Storage.ROOT + StorageIdParam.PATH
    )
    StoragesResponse<StorageItem> getStorageItemsByStorageId(@PathVariable(StorageIdParam.NAME) StorageIdParam storageIdParam) throws IrdenAppClientException;


    @PostExchange(
            url = IrdenAppRoutes.Storage.REQUEST
    )
    void makeTransferRequest(@RequestBody StorageTransferRequest request)  throws IrdenAppClientException;
}