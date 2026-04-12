package irden.space.proxy.plugin.runtime;

import irden.space.proxy.plugin.api.PluginSessionContext;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketEnvelope;
import irden.space.proxy.protocol.payload.registry.PacketParser;

import java.util.function.BiConsumer;

public class DefaultPluginSessionContext implements PluginSessionContext {

    private final String sessionId;
    private final String clientIp;
    private final boolean clientZstdEnabled;
    private final boolean upstreamZstdEnabled;
    private final int openProtocolVersion;
    private final BiConsumer<PacketDirection, PacketEnvelope> packetSender;

    public DefaultPluginSessionContext(
            String sessionId,
            String clientIp,
            boolean clientZstdEnabled,
            boolean upstreamZstdEnabled
    ) {
        this(
                sessionId,
                clientIp,
                clientZstdEnabled,
                upstreamZstdEnabled,
                PacketParser.UNKNOWN_OPEN_PROTOCOL_VERSION,
                null
        );
    }

    public DefaultPluginSessionContext(
            String sessionId,
            String clientIp,
            boolean clientZstdEnabled,
            boolean upstreamZstdEnabled,
            int openProtocolVersion
    ) {
        this(sessionId, clientIp, clientZstdEnabled, upstreamZstdEnabled, openProtocolVersion, null);
    }

    public DefaultPluginSessionContext(
            String sessionId,
            String clientIp,
            boolean clientZstdEnabled,
            boolean upstreamZstdEnabled,
            BiConsumer<PacketDirection, PacketEnvelope> packetSender
    ) {
        this(
                sessionId,
                clientIp,
                clientZstdEnabled,
                upstreamZstdEnabled,
                PacketParser.UNKNOWN_OPEN_PROTOCOL_VERSION,
                packetSender
        );
    }

    public DefaultPluginSessionContext(
            String sessionId,
            String clientIp,
            boolean clientZstdEnabled,
            boolean upstreamZstdEnabled,
            int openProtocolVersion,
            BiConsumer<PacketDirection, PacketEnvelope> packetSender
    ) {
        this.sessionId = sessionId;
        this.clientIp = clientIp;
        this.clientZstdEnabled = clientZstdEnabled;
        this.upstreamZstdEnabled = upstreamZstdEnabled;
        this.openProtocolVersion = openProtocolVersion;
        this.packetSender = packetSender;
    }

    @Override
    public String sessionId() {
        return sessionId;
    }

    @Override
    public String clientIp() {
        return clientIp;
    }

    @Override
    public boolean clientZstdEnabled() {
        return clientZstdEnabled;
    }

    @Override
    public boolean upstreamZstdEnabled() {
        return upstreamZstdEnabled;
    }

    @Override
    public int openProtocolVersion() {
        return openProtocolVersion;
    }

    @Override
    public void send(PacketDirection direction, PacketEnvelope envelope) {
        if (packetSender == null) {
            PluginSessionContext.super.send(direction, envelope);
            return;
        }

        packetSender.accept(direction, envelope);
    }
}

