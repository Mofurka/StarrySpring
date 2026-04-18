package irden.space.boot;


import irden.space.proxy.application.ProxyRuntimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(3)
@RequiredArgsConstructor
public class ProxyRuntimeStartup implements CommandLineRunner {

    private final ProxyRuntimeService proxyRuntimeService;

    @Override
    public void run(String... args) {
        proxyRuntimeService.start();
    }
}
