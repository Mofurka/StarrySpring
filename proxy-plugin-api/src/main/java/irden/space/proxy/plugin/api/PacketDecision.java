package irden.space.proxy.plugin.api;


import irden.space.proxy.protocol.packet.PacketEnvelope;
import org.jetbrains.annotations.NotNull;

public sealed interface PacketDecision
        permits ForwardPacketDecision, DropPacketDecision, ReplacePacketDecision {
    default boolean isForward() {
        return this instanceof ForwardPacketDecision;
    }
    default boolean isDrop() {
        return this instanceof DropPacketDecision;
    }
    default boolean isReplace() {
        return this instanceof ReplacePacketDecision;
    }
    static ForwardPacketDecision forward() {
        return ForwardPacketDecision.INSTANCE;
    }
    static DropPacketDecision cancel() {
        return DropPacketDecision.INSTANCE;
    }
    static ReplacePacketDecision replace(@NotNull PacketEnvelope defaultValue) {
        return new ReplacePacketDecision(defaultValue);
    }
}