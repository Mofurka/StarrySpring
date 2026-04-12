package irden.space.proxy.plugin.api;


import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketType;

import java.util.*;

public interface PacketInterceptorRegistry {

    void register(PacketInterceptor interceptor);

    default void register(PacketType packetType, PacketInterceptorHandler handler) {
        Objects.requireNonNull(packetType, "packetType");
        register(EnumSet.of(packetType), List.of(), handler);
    }

    default void register(PacketType packetType, PacketDirection direction, PacketInterceptorHandler handler) {
        Objects.requireNonNull(direction, "direction");
        register(EnumSet.of(packetType), List.of(direction), handler);
    }

    @SuppressWarnings("unused")
    default void register(Map<PacketType, PacketInterceptorHandler> handlers) {
        Objects.requireNonNull(handlers, "handlers");
        handlers.forEach(this::register);
    }

    @SuppressWarnings("unused")
    default void register(Map<PacketType, PacketInterceptorHandler> handlers, PacketDirection direction) {
        Objects.requireNonNull(handlers, "handlers");
        Objects.requireNonNull(direction, "direction");
        handlers.forEach((packetType, handler) -> register(packetType, direction, handler));
    }

    default void register(Collection<PacketType> packetTypes, PacketInterceptorHandler handler) {
        register(packetTypes, List.of(), handler);
    }

    default void register(Collection<PacketType> packetTypes, PacketDirection direction, PacketInterceptorHandler handler) {
        Objects.requireNonNull(direction, "direction");
        register(packetTypes, List.of(direction), handler);
    }

    default void register(Collection<PacketType> packetTypes, Collection<PacketDirection> directions, PacketInterceptorHandler handler) {
        Objects.requireNonNull(packetTypes, "packetTypes");
        Objects.requireNonNull(directions, "directions");
        Objects.requireNonNull(handler, "handler");

        EnumSet<PacketType> registeredPacketTypes = EnumSet.noneOf(PacketType.class);
        registeredPacketTypes.addAll(packetTypes);
        EnumSet<PacketDirection> registeredDirections = EnumSet.noneOf(PacketDirection.class);
        registeredDirections.addAll(directions);

        if (registeredPacketTypes.isEmpty()) {
            throw new IllegalArgumentException("packetTypes must not be empty");
        }

        register(new PacketInterceptor() {
            @Override
            public boolean supports(PacketInterceptionContext context) {
                PacketType packetType = context.envelope().packetType();
                PacketDirection direction = context.direction();
                return packetType != null
                        && registeredPacketTypes.contains(packetType)
                        && (registeredDirections.isEmpty() || direction != null && registeredDirections.contains(direction));
            }

            @Override
            public PacketDecision intercept(PacketInterceptionContext context) {
                return handler.intercept(context);
            }
        });
    }

    default void registerAnnotated(Object handlerTarget) {
        PacketHandlerRegistrar.register(this, handlerTarget);
    }

    List<PacketInterceptor> getAll();
}
