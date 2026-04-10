package irden.space.proxy.application.runtime;

import irden.space.proxy.application.port.out.SessionRegistry;
import irden.space.proxy.domain.session.ProxySession;
import irden.space.proxy.domain.session.SessionState;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
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

    public PacketForwarder(
            InputStream source,
            OutputStream target,
            SessionRegistry sessionRegistry,
            PacketDirection packetDirection,
            PacketDispatcher packetDispatcher,
            ProxySessionRuntimeContext context,
            SwitchableSessionTransport transport
    ) {
        this.context = context;
        this.session = context.session();
        this.source = source;
        this.target = target;
        this.transport = transport;
        this.clientSocket = context.clientSocket();
        this.upstreamSocket = context.upstreamSocket();
        this.sessionRegistry = sessionRegistry;
        this.packetDirection = packetDirection;
        this.packetDispatcher = packetDispatcher;
    }

    @Override
    public void run() {
        try {
            while (!clientSocket.isClosed() && !upstreamSocket.isClosed()) {
                PacketEnvelope envelope = readPacket();
                if (envelope == null) continue;


                Object parsed = null;

                if (envelope.packetType() != null) {
                    parsed = parsePacket(envelope);
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

                boolean negotiatedZstd = packetDirection == PacketDirection.TO_CLIENT
                        && envelope.packetType() == PacketType.PROTOCOL_RESPONSE
                        && parsed instanceof ProtocolResponse protocolResponse
                        && isZstdNegotiated(protocolResponse);

                if (negotiatedZstd) {
                    log.info("Session {} negotiated ZSTD via ProtocolResponse", session.getId());
                    switchSessionToZstd();
                }

                if (envelope.packetType().equals(PacketType.CHAT_SENT) || envelope.packetType().equals(PacketType.CHAT_RECEIVED)) {
                    log.info("[{}]", parsed);
                }

                transport.write(target, envelope);
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
        synchronized (context.session()) {
            if (context.clientSideTransport().isZstdReadEnabled()
                    && context.clientSideTransport().isZstdWriteEnabled()
                    && context.upstreamSideTransport().isZstdReadEnabled()
                    && context.upstreamSideTransport().isZstdWriteEnabled()) {
                return;
            }

            context.clientSideTransport().enableZstdRead();
            context.upstreamSideTransport().enableZstdRead();

            waitForPlainReadersToDrain();

            context.clientSideTransport().enableZstdWrite(0);
            context.upstreamSideTransport().enableZstdWrite(1);

            context.session().enableClientZstd();
            context.session().enableUpstreamZstd();

            log.info("Session {} switched to ZSTD transport mode", context.session().getId());
        }
    }

    private Object parsePacket(PacketEnvelope envelope) {
        Object parsed = null;
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
        return parsed;
    }


    private PacketEnvelope readPacket() throws IOException {
        PacketEnvelope envelope;
        try {
            envelope = transport.read(source, packetDirection);
        } catch (SocketTimeoutException _) {
            return null;
        }
        return envelope;
    }

    private void waitForPlainReadersToDrain() {
        long gracePeriodMillis = resolveReadSwitchGracePeriodMillis();

        try {
            Thread.sleep(gracePeriodMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while switching session transport to ZSTD", e);
        }
    }

    private long resolveReadSwitchGracePeriodMillis() {
        int clientTimeout = readSocketTimeout(context.clientSocket());
        int upstreamTimeout = readSocketTimeout(context.upstreamSocket());
        int maxTimeout = Math.max(clientTimeout, upstreamTimeout);

        if (maxTimeout <= 0) {
            return 50L;
        }

        return maxTimeout + 50L;
    }

    private int readSocketTimeout(Socket socket) {
        try {
            return socket.getSoTimeout();
        } catch (SocketException e) {
            log.debug("Failed to read socket timeout for session {}: {}", session.getId(), e.getMessage());
            return 0;
        }
    }

    private boolean isZstdNegotiated(ProtocolResponse response) {
        if (!(response.info() instanceof MapVariantValue(Map<String, VariantValue> values))) {
            return false;
        }

        VariantValue compression = values.get("compression");
        return compression instanceof StringVariantValue(String value)
                && "Zstd".equalsIgnoreCase(value);
    }

    private void closeSession() {
        synchronized (session) {
            if (session.getState().equals(SessionState.DISCONNECTED)) {
                return;
            }

            try {
                session.markDisconnecting();
            } catch (Exception _) {
                log.warn("Failed to mark session {} as DISCONNECTING", session.getId());
            }

            try {
                clientSocket.close();
            } catch (Exception _) {
                log.warn("Failed to close client socket for session {}", session.getId());
            }

            try {
                upstreamSocket.close();
            } catch (Exception _) {
                log.warn("Failed to close upstream socket for session {}", session.getId());
            }

            try {
                session.markDisconnected();
            } catch (Exception _) {
                log.warn("Failed to mark session {} as DISCONNECTED", session.getId());
            }

            sessionRegistry.remove(session.getId());

            log.info("Session {} closed and removed", session.getId());
        }
    }
}