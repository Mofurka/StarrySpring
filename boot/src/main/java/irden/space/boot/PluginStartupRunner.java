package irden.space.boot;

import irden.space.proxy.plugin.runtime.PluginManager;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
@RequiredArgsConstructor
public class PluginStartupRunner implements CommandLineRunner {

    private final PluginManager pluginManager;

    @Override
    public void run(String... args) {
        pluginManager.loadAndStart();
    }
}
