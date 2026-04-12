package irden.space.proxy.plugin.api;

public interface PacketInterceptionService {

    PacketDecision apply(PacketInterceptionContext context);
}

