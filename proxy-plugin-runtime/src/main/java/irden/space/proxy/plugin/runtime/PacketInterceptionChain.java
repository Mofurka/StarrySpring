package irden.space.proxy.plugin.runtime;


import irden.space.proxy.plugin.api.*;
import irden.space.proxy.protocol.packet.PacketEnvelope;

import java.util.ArrayList;
import java.util.List;

public class PacketInterceptionChain implements PacketInterceptionService {

    private final PacketInterceptorRegistry interceptorRegistry;

    public PacketInterceptionChain(PacketInterceptorRegistry interceptorRegistry) {
        this.interceptorRegistry = interceptorRegistry;
    }


    @Override
    public PacketDecision apply(PacketInterceptionContext context) {
        List<Runnable> afterForwardCallbacks = new ArrayList<>();

        for (PacketInterceptor interceptor : interceptorRegistry.getAll()) {
            if (!interceptor.supports(context)) {
                continue;
            }

            PacketDecision decision = interceptor.intercept(context);

            if (decision instanceof DropPacketDecision) {
                return decision;
            }

            if (decision instanceof ReplacePacketDecision(PacketEnvelope envelope, Runnable afterReplace)) {
                if (afterForwardCallbacks.isEmpty()) {
                    return decision;
                }
                collect(afterForwardCallbacks, afterReplace);
                return new ReplacePacketDecision(envelope, combine(afterForwardCallbacks));
            }

            if (decision instanceof ForwardPacketDecision(Runnable afterForward)) {
                collect(afterForwardCallbacks, afterForward);
            }
        }

        if (afterForwardCallbacks.isEmpty()) {
            return ForwardPacketDecision.INSTANCE;
        }

        return new ForwardPacketDecision(combine(afterForwardCallbacks));
    }

    private void collect(List<Runnable> callbacks, Runnable callback) {
        if (callback != null) {
            callbacks.add(callback);
        }
    }


    private Runnable combine(List<Runnable> callbacks) {
        if (callbacks.size() == 1) {
            return callbacks.getFirst();
        }

        List<Runnable> orderedCallbacks = List.copyOf(callbacks);
        return () -> {
            RuntimeException firstFailure = null;
            for (Runnable callback : orderedCallbacks) {
                try {
                    callback.run();
                } catch (RuntimeException e) {
                    if (firstFailure == null) {
                        firstFailure = e;
                    } else {
                        firstFailure.addSuppressed(e);
                    }
                }
            }
            if (firstFailure != null) {
                throw firstFailure;
            }
        };
    }
}
