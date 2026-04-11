package irden.space.proxy.plugin_runtime;


import irden.space.proxy.plugin_api.*;

public class PacketInterceptionChain implements PacketInterceptionService {

    private final PacketInterceptorRegistry interceptorRegistry;

    public PacketInterceptionChain(PacketInterceptorRegistry interceptorRegistry) {
        this.interceptorRegistry = interceptorRegistry;
    }

    @Override
    public PacketDecision apply(PacketInterceptionContext context) {
        PacketDecision currentDecision = ForwardPacketDecision.INSTANCE;

        for (PacketInterceptor interceptor : interceptorRegistry.getAll()) {
            if (!interceptor.supports(context)) {
                continue;
            }

            currentDecision = interceptor.intercept(context);

            if (currentDecision instanceof DropPacketDecision) {
                return currentDecision;
            }

            if (currentDecision instanceof ReplacePacketDecision) {
                return currentDecision;
            }
        }

        return currentDecision;
    }
}
