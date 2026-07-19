package irden.space.proxy.plugin.site.web.rest.v1;

import irden.space.proxy.plugin.site.web.rest.v1.dto.OnlinePlayerInfoDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
@RequestMapping("/api/v1/online")
@RequiredArgsConstructor
public class OnlineController {
    private final OnlinePlayersHandler onlinePlayersHandler;


    @PreAuthorize("hasRole('SITE')")
    @GetMapping
    public ResponseEntity<Collection<OnlinePlayerInfoDto>> online() {
        return ResponseEntity.ok(onlinePlayersHandler.handleRequest(false));
    }

    @GetMapping("/public")
    public ResponseEntity<Collection<OnlinePlayerInfoDto>> onlinePublic() {
        return ResponseEntity.ok(onlinePlayersHandler.handleRequest(true));
    }
}
