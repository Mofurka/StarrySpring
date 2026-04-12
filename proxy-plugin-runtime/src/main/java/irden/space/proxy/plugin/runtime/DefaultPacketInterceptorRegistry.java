package irden.space.proxy.plugin.runtime;


import irden.space.proxy.plugin.api.PacketInterceptor;
import irden.space.proxy.plugin.api.PacketInterceptorRegistry;

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