package irden.space.proxy.plugin.site.web.rest.v1;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1")
public class TestController {

    @GetMapping("/public")
    public String pub() {
        return  "Hello from public test controller!";
    }

    @GetMapping("/secured")
    @PreAuthorize("hasRole('SITE')")
    public String sec(Principal principal) {
        return  "Hello from secured test controller! %s".formatted(principal.getName());
    }


}
