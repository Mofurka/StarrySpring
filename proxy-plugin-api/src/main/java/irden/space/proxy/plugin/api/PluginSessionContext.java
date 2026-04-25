package irden.space.proxy.plugin.api;


import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketEnvelope;
import irden.space.proxy.protocol.packet.PacketEnvelopes;
import irden.space.proxy.protocol.packet.PacketType;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public interface PluginSessionContext {

    String sessionId();

    String clientIp();

    boolean clientZstdEnabled();

    boolean upstreamZstdEnabled();

    default PermissionView permissions() {
        return PermissionView.EMPTY;
    }

    default int openProtocolVersion() {
        return PacketParser.LEGACY_PROTOCOL_VERSION;
    }

    default void send(PacketDirection direction, PacketEnvelope envelope) {
        throw new UnsupportedOperationException("Packet sending is not available for this session context");
    }

    default void send(PacketDirection direction, PacketType packetType, Object payload) {
        send(direction, PacketEnvelopes.fromPayload(packetType, payload, openProtocolVersion(), direction));
    }

    default void send(PacketDirection direction, PacketType packetType, byte[] payload, boolean compressed) {
        send(direction, PacketEnvelopes.fromRawPayload(packetType, payload, compressed, direction));
    }

    default void sendToClient(PacketEnvelope envelope) {
        send(PacketDirection.TO_CLIENT, envelope);
    }

    default void sendToClient(PacketType packetType, Object payload) {
        send(PacketDirection.TO_CLIENT, packetType, payload);
    }

    default void sendToClient(PacketType packetType, byte[] payload, boolean compressed) {
        send(PacketDirection.TO_CLIENT, packetType, payload, compressed);
    }

    default void sendToServer(PacketEnvelope envelope) {
        send(PacketDirection.TO_SERVER, envelope);
    }

    default void sendToServer(PacketType packetType, Object payload) {
        send(PacketDirection.TO_SERVER, packetType, payload);
    }

    default void sendToServer(PacketType packetType, byte[] payload, boolean compressed) {
        send(PacketDirection.TO_SERVER, packetType, payload, compressed);
    }
}
