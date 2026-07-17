package irden.space.proxy.plugin.site.web.client;

import irden.space.proxy.plugin.site.web.client.dto.LinkPlayerRequest;
import irden.space.proxy.plugin.site.web.client.dto.LinkPlayerResponse;
import irden.space.proxy.plugin.site.web.client.exceptions.IrdenAppClientException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.PostExchange;

public interface IrdenAppClient {

    @PostExchange(
            url = "/api/link",
            contentType = MediaType.APPLICATION_JSON_VALUE
    )
    LinkPlayerResponse link(@RequestBody LinkPlayerRequest request) throws IrdenAppClientException;

    @DeleteExchange(
            url = "/api/link"
    )
    void unlink(@RequestParam("uuid") String uuid) throws IrdenAppClientException;

}