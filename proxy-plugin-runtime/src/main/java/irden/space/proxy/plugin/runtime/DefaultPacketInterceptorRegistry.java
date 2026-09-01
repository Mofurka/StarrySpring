package irden.space.proxy.plugin.runtime;


import irden.space.proxy.plugin.api.PacketInterceptor;
import irden.space.proxy.plugin.api.PacketInterceptorRegistry;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DefaultPacketInterceptorRegistry implements PacketInterceptorRegistry {

    private final CopyOnWriteArrayList<PacketInterceptor> interceptors = new CopyOnWriteArrayList<>();


    private final List<PacketInterceptor> view = Collections.unmodifiableList(interceptors);

    @Override
    public void register(PacketInterceptor interceptor) {
        interceptors.add(interceptor);
    }

    @Override
    public void unregister(PacketInterceptor interceptor) {
        interceptors.remove(interceptor);
    }


    @Override
    public List<PacketInterceptor> getAll() {
        return view;
    }
}
