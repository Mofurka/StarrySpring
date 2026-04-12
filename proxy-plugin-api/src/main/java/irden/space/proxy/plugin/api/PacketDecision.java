package irden.space.proxy.plugin.api;


public sealed interface PacketDecision
        permits ForwardPacketDecision, DropPacketDecision, ReplacePacketDecision {
}