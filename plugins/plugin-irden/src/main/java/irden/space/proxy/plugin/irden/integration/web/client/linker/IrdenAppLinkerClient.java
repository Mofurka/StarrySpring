package irden.space.proxy.plugin.irden.integration.web.client.linker;

import irden.space.proxy.plugin.irden.integration.web.client.constants.IrdenAppRoutes;
import irden.space.proxy.plugin.irden.integration.web.client.exceptions.IrdenAppClientException;
import irden.space.proxy.plugin.irden.integration.web.client.linker.dto.LinkPlayerRequest;
import irden.space.proxy.plugin.irden.integration.web.client.linker.dto.LinkPlayerResponse;
import irden.space.proxy.plugin.irden.integration.web.dto.player_uuid.PlayerUuidParam;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.PostExchange;

public interface IrdenAppLinkerClient {

    @PostExchange(
            url = IrdenAppRoutes.Linker.LINK,
            contentType = MediaType.APPLICATION_JSON_VALUE
    )
    LinkPlayerResponse link(@RequestBody LinkPlayerRequest request) throws IrdenAppClientException;

    @DeleteExchange(
            url = IrdenAppRoutes.Linker.LINK
    )
    void unlink(@RequestParam(PlayerUuidParam.NAME) PlayerUuidParam uuid) throws IrdenAppClientException;

}