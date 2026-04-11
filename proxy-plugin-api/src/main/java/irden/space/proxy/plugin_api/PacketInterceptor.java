package irden.space.proxy.plugin_api;


public interface PacketInterceptor {

    boolean supports(PacketInterceptionContext context);

    PacketDecision intercept(PacketInterceptionContext context);
}
