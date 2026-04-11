package irden.space.proxy.plugin_runtime;


import irden.space.proxy.plugin_api.PacketInterceptor;
import irden.space.proxy.plugin_api.PacketInterceptorRegistry;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DefaultPacketInterceptorRegistry implements PacketInterceptorRegistry {

    private final CopyOnWriteArrayList<PacketInterceptor> interceptors = new CopyOnWriteArrayList<>();

    @Override
    public void register(PacketInterceptor interceptor) {
        interceptors.add(interceptor);
    }

    @Override
    public List<PacketInterceptor> getAll() {
        return List.copyOf(interceptors);
    }
}