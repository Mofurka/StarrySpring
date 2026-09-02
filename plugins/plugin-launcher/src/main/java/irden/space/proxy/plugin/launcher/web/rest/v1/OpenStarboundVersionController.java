package irden.space.proxy.plugin.launcher.web.rest.v1;

import irden.space.proxy.plugin.launcher.services.OpenStarboundVersionService;
import irden.space.proxy.plugin.launcher.web.rest.v1.constants.RestRoutes;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RestRoutes.OpenStarboundV1.PUBLIC)
@RequiredArgsConstructor
public class OpenStarboundVersionController {
    private final OpenStarboundVersionService openStarboundVersionService;


    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public String version() {
        return openStarboundVersionService.openStarboundVersion();
    }

}
