package irden.space.proxy.application;

import lombok.RequiredArgsConstructor;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProxyRuntimeLifecycle implements SmartLifecycle {

    private final ProxyRuntimeService proxyRuntimeService;

    private volatile boolean running;

    @Override
    public void start() {
        proxyRuntimeService.start();
        running = true;
    }

    @Override
    public boolean isAutoStartup() {
        return false;
    }

    @Override
    public boolean isPauseable() {
        return false;
    }

    @Override
    public void stop() {
        proxyRuntimeService.stop();
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}