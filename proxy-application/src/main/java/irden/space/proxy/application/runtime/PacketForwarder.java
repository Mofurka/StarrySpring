package irden.space.proxy.application.runtime;

import irden.space.proxy.application.port.out.SessionRegistry;
import irden.space.proxy.domain.session.ProxySession;
import irden.space.proxy.protocol.codec.variant.MapVariantValue;
import irden.space.proxy.protocol.codec.variant.StringVariantValue;
import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketEnvelope;
import irden.space.proxy.protocol.packet.PacketType;
import irden.space.proxy.protocol.payload.packet.protocol_response.ProtocolResponse;
import irden.space.proxy.protocol.payload.registry.PacketDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;

public class PacketForwarder implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(PacketForwarder.class);

    private final ProxySession session;
    private final InputStream source;
    private final OutputStream target;
    private final Socket clientSocket;
    private final Socket upstreamSocket;
    private final SessionRegistry sessionRegistry;
    private final PacketDirection packetDirection;
    private final PacketDispatcher packetDispatcher;
    private final ProxySessionRuntimeContext context;
    private final SwitchableSessionTransport transport;

    private boolean shouldStopAfterWrite = false;

    public PacketForwarder(
            ProxySession session,
            InputStream source,
            OutputStream target,
            Socket clientSocket,
            Socket upstreamSocket,
            SessionRegistry sessionRegistry,
            PacketDirection packetDirection,
            PacketDispatcher packetDispatcher,
            ProxySessionRuntimeContext context,
            SwitchableSessionTransport transport
    ) {
        this.session = session;
        this.source = source;
        this.target = target;
        this.clientSocket = clientSocket;
        this.upstreamSocket = upstreamSocket;
        this.sessionRegistry = sessionRegistry;
        this.packetDirection = packetDirection;
        this.packetDispatcher = packetDispatcher;

        this.context = context;
        this.transport = transport;
    }

    @Override
    public void run() {
        try {
            while (!clientSocket.isClosed() && !upstreamSocket.isClosed()) {
                PacketEnvelope envelope = transport.read(source, packetDirection);

                Object parsed = null;

                if (envelope.packetType() != null) {
                    try {
                        parsed = packetDispatcher.parse(envelope);
                    } catch (Exception e) {
                        log.debug(
                                "[{}] session={} parse failed for type={}: {}",
                                packetDirection,
                                session.getId(),
                                envelope.packetType(),
                                e.getMessage()
                        );
                    }
                }
                log.debug(
                        "[{}] session={} rawType={} type={} size={} compressed={} parsed={}",
                        packetDirection,
                        session.getId(),
                        envelope.rawPacketTypeId(),
                        envelope.packetType(),
                        envelope.payloadSize(),
                        envelope.compressed(),
                        parsed
                );

                transport.write(target, envelope);

                if (packetDirection == PacketDirection.TO_CLIENT
                        && envelope.packetType() == PacketType.PROTOCOL_RESPONSE
                        && parsed instanceof ProtocolResponse protocolResponse) {

                    if (isZstdNegotiated(protocolResponse)) {
                        log.info("Session {} negotiated ZSTD via ProtocolResponse", session.getId());
                        switchSessionToZstd();
                    }
                }
                if (shouldStopAfterWrite) {
                    log.info("Session {} stopping after forwarding ProtocolResponse with ZSTD negotiation", session.getId());
                    closeSession();
                    return;
                }
            }
        } catch (SocketException _) {
            log.info("[{}] socket closed for session {}", packetDirection, session.getId());
        } catch (Exception e) {
            log.warn("[{}] forwarding stopped for session {}: {}", packetDirection, session.getId(), e.getMessage(), e);
        } finally {
            closeSession();
        }
    }

    private void switchSessionToZstd() {
        context.switchToZstd();
        log.info("Session {} switched to ZSTD transport mode", context.session().getId());
    }

    private boolean isZstdNegotiated(ProtocolResponse response) {
        if (!(response.info() instanceof MapVariantValue(Map<String, VariantValue> value1))) {
            return false;
        }

        VariantValue compression = value1.get("compression");
        return compression instanceof StringVariantValue(String value)
                && "Zstd".equalsIgnoreCase(value);
    }

    private void closeSession() {
        synchronized (session) {
            if (session.getState().name().equals("DISCONNECTED")) {
                return;
            }

            try {
                session.markDisconnecting();
            } catch (Exception _) {
            }

            try {
                clientSocket.close();
            } catch (Exception _) {
            }

            try {
                upstreamSocket.close();
            } catch (Exception _) {
            }

            try {
                session.markDisconnected();
            } catch (Exception _) {
            }

            sessionRegistry.remove(session.getId());

            log.info("Session {} closed and removed", session.getId());
        }
    }
}
