package irden.space.proxy.plugin.api;

import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public final class PacketHandlerRegistrar {

    private PacketHandlerRegistrar() {
    }

    public static void register(PacketInterceptorRegistry registry, Object handlerTarget) {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(handlerTarget, "handlerTarget");

        List<MethodBinding> methodBindings = Arrays.stream(handlerTarget.getClass().getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(PacketHandler.class))
                .map(PacketHandlerRegistrar::createBinding)
                .toList();

        if (methodBindings.isEmpty()) {
            throw new IllegalArgumentException("No @PacketHandler methods found on " + handlerTarget.getClass().getName());
        }

        validateNoOverlaps(handlerTarget.getClass(), methodBindings);

        for (MethodBinding methodBinding : methodBindings) {
            if (!methodBinding.method().trySetAccessible()) {
                throw new IllegalStateException("Cannot access @PacketHandler method " + methodBinding.method());
            }

            registry.register(
                    methodBinding.packetTypes(),
                    methodBinding.directions(),
                    context -> invoke(handlerTarget, methodBinding.method(), context)
            );
        }
    }

    private static MethodBinding createBinding(Method method) {
        validateMethodSignature(method);

        PacketHandler annotation = method.getAnnotation(PacketHandler.class);

        EnumSet<PacketType> packetTypes = EnumSet.noneOf(PacketType.class);
        packetTypes.addAll(List.of(annotation.value()));

        if (packetTypes.isEmpty()) {
            throw new IllegalArgumentException("@PacketHandler must declare at least one PacketType on " + method);
        }

        EnumSet<PacketDirection> directions = EnumSet.noneOf(PacketDirection.class);
        directions.addAll(List.of(annotation.direction()));

        return new MethodBinding(method, packetTypes, directions);
    }

    private static void validateMethodSignature(Method method) {
        if (Modifier.isStatic(method.getModifiers())) {
            throw new IllegalArgumentException("@PacketHandler method must not be static: " + method);
        }

        if (method.getParameterCount() != 1 || !PacketInterceptionContext.class.equals(method.getParameterTypes()[0])) {
            throw new IllegalArgumentException(
                    "@PacketHandler method must declare exactly one PacketInterceptionContext parameter: " + method
            );
        }

        if (!PacketDecision.class.isAssignableFrom(method.getReturnType())) {
            throw new IllegalArgumentException("@PacketHandler method must return PacketDecision: " + method);
        }
    }

    private static void validateNoOverlaps(Class<?> handlerType, List<MethodBinding> methodBindings) {
        Set<PacketType> anyDirectionCoverage = EnumSet.noneOf(PacketType.class);
        Map<PacketType, Set<PacketDirection>> directionalCoverage = new EnumMap<>(PacketType.class);

        for (MethodBinding methodBinding : methodBindings) {
            for (PacketType packetType : methodBinding.packetTypes()) {
                if (methodBinding.directions().isEmpty()) {
                    if (anyDirectionCoverage.contains(packetType)
                            || directionalCoverage.containsKey(packetType) && !directionalCoverage.get(packetType).isEmpty()) {
                        throw new IllegalArgumentException(overlapMessage(handlerType, packetType, methodBinding.method()));
                    }

                    anyDirectionCoverage.add(packetType);
                    continue;
                }

                if (anyDirectionCoverage.contains(packetType)) {
                    throw new IllegalArgumentException(overlapMessage(handlerType, packetType, methodBinding.method()));
                }

                Set<PacketDirection> coveredDirections = directionalCoverage.computeIfAbsent(
                        packetType,
                        ignored -> EnumSet.noneOf(PacketDirection.class)
                );

                for (PacketDirection direction : methodBinding.directions()) {
                    if (!coveredDirections.add(direction)) {
                        throw new IllegalArgumentException(overlapMessage(handlerType, packetType, methodBinding.method()));
                    }
                }
            }
        }
    }

    private static PacketDecision invoke(Object handlerTarget, Method method, PacketInterceptionContext context) {
        try {
            return (PacketDecision) method.invoke(handlerTarget, context);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("Failed to invoke @PacketHandler method " + method, cause);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to invoke @PacketHandler method " + method, e);
        }
    }

    private static String overlapMessage(Class<?> handlerType, PacketType packetType, Method method) {
        return "Overlapping @PacketHandler registration for packetType="
                + packetType
                + " on "
                + handlerType.getName()
                + " via method "
                + method.getName();
    }

    private record MethodBinding(Method method, Set<PacketType> packetTypes, Set<PacketDirection> directions) {
    }
}
