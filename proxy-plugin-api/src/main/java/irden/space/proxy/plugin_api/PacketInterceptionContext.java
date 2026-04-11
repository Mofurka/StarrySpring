package irden.space.proxy.plugin_api;


import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketEnvelope;


public record PacketInterceptionContext(
        PluginSessionContext session,
        PacketEnvelope envelope,
        Object parsedPayload,
        PacketDirection direction
) {
}