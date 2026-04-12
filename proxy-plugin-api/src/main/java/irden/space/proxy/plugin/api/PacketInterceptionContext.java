package irden.space.proxy.plugin.api;


import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketEnvelope;
import irden.space.proxy.protocol.packet.PacketEnvelopes;


public record PacketInterceptionContext(
        PluginSessionContext session,
        PacketEnvelope envelope,
        Object parsedPayload,
        PacketDirection direction
) {

    public PacketEnvelope envelopeWithPayload(Object payload) {
        return PacketEnvelopes.rewrite(envelope, payload, session.openProtocolVersion());
    }

    public PacketEnvelope envelopeWithRawPayload(byte[] payload) {
        return PacketEnvelopes.rewriteRawPayload(envelope, payload);
    }

    public PacketDecision replaceWithPayload(Object payload) {
        return PacketDecision.replace(envelopeWithPayload(payload));
    }

    public PacketDecision replaceWithRawPayload(byte[] payload) {
        return PacketDecision.replace(envelopeWithRawPayload(payload));
    }
}