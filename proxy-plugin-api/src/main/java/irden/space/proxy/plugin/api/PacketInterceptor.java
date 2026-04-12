package irden.space.proxy.plugin.api;


public interface PacketInterceptor {

    boolean supports(PacketInterceptionContext context);

    PacketDecision intercept(PacketInterceptionContext context);
}
