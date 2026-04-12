package irden.space.proxy.plugin.api;


import java.util.List;

public interface PacketInterceptorRegistry {

    void register(PacketInterceptor interceptor);

    List<PacketInterceptor> getAll();
}
