package irden.space.proxy.plugin.site.web.rest.v1.online;

import irden.space.proxy.plugin.site.web.rest.v1.constants.RestRoutes;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
@PreAuthorize("hasRole('SITE')")
@RequestMapping(RestRoutes.OnlineV1.PRIVATE)
@RequiredArgsConstructor
public class OnlineController {
    private final OnlinePlayersHandler onlinePlayersHandler;

    @GetMapping
    public ResponseEntity<Collection<OnlinePlayerInfoDto>> online() {
        return ResponseEntity.ok(onlinePlayersHandler.handleRequest(false));
    }
}
