package irden.space.proxy.plugin.api;


import irden.space.proxy.protocol.packet.PacketEnvelope;

public record ReplacePacketDecision(PacketEnvelope envelope) implements PacketDecision {
}