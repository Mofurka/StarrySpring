package irden.space.proxy.plugin.api;

@FunctionalInterface
public interface PacketInterceptorHandler {

    PacketDecision intercept(PacketInterceptionContext context);
}
