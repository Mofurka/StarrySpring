package irden.space.boot;

import irden.space.proxy.application.ProxyRuntimeService;
import irden.space.proxy.plugin.runtime.PluginManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
@RequiredArgsConstructor
public class PluginStartupRunner implements CommandLineRunner, DisposableBean {

    private final PluginManager pluginManager;
    private final ProxyRuntimeService proxyRuntimeService;

    @Override
    public void run(String... args) {
        pluginManager.loadAndStart();
    }

    @Override
    public void destroy() {
        try {
            pluginManager.notifyStopping();
            proxyRuntimeService.drainSessions();
        } finally {
            pluginManager.closeContainers();
        }
    }
}
