package irden.space.proxy.plugin_api;


public sealed interface PacketDecision
        permits ForwardPacketDecision, DropPacketDecision, ReplacePacketDecision {
}