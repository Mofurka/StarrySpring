package irden.space.proxy.plugin_api;

public interface PacketInterceptionService {

    PacketDecision apply(PacketInterceptionContext context);
}

