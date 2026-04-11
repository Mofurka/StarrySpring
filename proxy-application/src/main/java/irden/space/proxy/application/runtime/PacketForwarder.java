package irden.space.proxy.application.runtime;

import irden.space.proxy.application.port.out.SessionRegistry;
import irden.space.proxy.domain.session.ProxySession;
import irden.space.proxy.domain.session.SessionState;
import irden.space.proxy.domain.session.SessionTransportMode;
import irden.space.proxy.plugin_api.DropPacketDecision;
import irden.space.proxy.plugin_api.PacketDecision;
import irden.space.proxy.plugin_api.PacketInterceptionContext;
import irden.space.proxy.plugin_api.PacketInterceptionService;
import irden.space.proxy.plugin_api.PluginSessionContext;
import irden.space.proxy.plugin_api.ReplacePacketDecision;
import irden.space.proxy.plugin_api.DefaultPluginSessionContext;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class PacketForwarder implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(PacketForwarder.class);

    private final ProxySession session;
    private final InputStream source;
    private final OutputStream target;
    private final Socket clientSocket;
    private final Socket upstreamSocket;
    private final SessionRegistry sessionRegistry;
    private final PacketDirection packetDirection;
    private final ProxySessionRuntimeContext context;
    private final SwitchableSessionTransport transport;
    private final RuntimePacketInspector packetInspector;
    private final PacketInterceptionService packetInterceptionService;

    public PacketForwarder(
            InputStream source,
            OutputStream target,
            SessionRegistry sessionRegistry,
            PacketDirection packetDirection,
            ProxySessionRuntimeContext context,
            SwitchableSessionTransport transport,
            RuntimePacketInspector packetInspector,
            PacketInterceptionService packetInterceptionService
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
        this.packetInspector = packetInspector;
        this.packetInterceptionService = packetInterceptionService;
    }

    @Override
    public void run() {
        try {
            while (!clientSocket.isClosed() && !upstreamSocket.isClosed()) {
                PacketEnvelope envelope = readPacket();
                if (envelope == null) {
                    continue;
                }

                PacketInspectionResult inspection = packetInspector.inspect(envelope, packetDirection);

                log.debug(
                        "[{}] session={} rawType={} type={} size={} compressed={} parsed={}",
                        packetDirection,
                        session.getId(),
                        envelope.rawPacketTypeId(),
                        envelope.packetType(),
                        envelope.payloadSize(),
                        envelope.compressed(),
                        inspection.parsed()
                );

                if (inspection.shouldLogPayload()) {
                    log.info("[{}] {}", packetDirection, inspection.parsed());
                }
                PluginSessionContext pluginSessionContext = new DefaultPluginSessionContext(
                        session.getId().uuid().toString(),
                        session.getClientIp(),
                        session.getClientCompression() == SessionTransportMode.ZSTD,
                        session.getUpstreamCompression() == SessionTransportMode.ZSTD
                );

                PacketInterceptionContext interceptionContext =
                        new PacketInterceptionContext(
                                pluginSessionContext,
                                envelope,
                                inspection.parsed(),
                                packetDirection
                        );

                PacketDecision decision = packetInterceptionService.apply(interceptionContext);

                if (decision instanceof DropPacketDecision) {
                    continue;
                }

                PacketEnvelope envelopeToWrite = envelope;
                if (decision instanceof ReplacePacketDecision(PacketEnvelope envelope1)) {
                    envelopeToWrite = envelope1;
                }

                if (shouldSwitchToZstd(envelope, envelopeToWrite, inspection)) {
                    log.info("Session {} negotiated ZSTD via ProtocolResponse", session.getId());
                    switchSessionToZstd();
                }

                transport.write(target, envelopeToWrite);
            }
        } catch (SocketException e) {
            log.info("[{}] socket exception for session {}: {}", packetDirection, session.getId(), e.getMessage());
        } catch (Exception e) {
            log.warn("[{}] forwarding stopped for session {}: {}", packetDirection, session.getId(), e.getMessage(), e);
        } finally {
            closeSession();
        }
    }

    private PacketEnvelope readPacket() throws IOException {
        try {
            return transport.read(source, packetDirection);
        } catch (SocketTimeoutException _) {
            return null;
        }
    }

    private boolean shouldSwitchToZstd(
            PacketEnvelope originalEnvelope,
            PacketEnvelope envelopeToWrite,
            PacketInspectionResult originalInspection
    ) {
        if (envelopeToWrite == originalEnvelope) {
            return originalInspection.negotiatedZstd();
        }

        return packetInspector.inspect(envelopeToWrite, packetDirection).negotiatedZstd();
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

    private void closeSession() {
        synchronized (session) {
            if (session.getState().equals(SessionState.DISCONNECTED)) {
                return;
            }

            try {
                session.markDisconnecting();
            } catch (Exception e) {
                log.warn("Failed to mark session {} as DISCONNECTING", session.getId());
            }

            try {
                clientSocket.close();
            } catch (Exception e) {
                log.warn("Failed to close client socket for session {}", session.getId());
            }

            try {
                upstreamSocket.close();
            } catch (Exception e) {
                log.warn("Failed to close upstream socket for session {}", session.getId());
            }

            try {
                session.markDisconnected();
            } catch (Exception e) {
                log.warn("Failed to mark session {} as DISCONNECTED", session.getId());
            }

            sessionRegistry.remove(session.getId());

            log.info("Session {} closed and removed", session.getId());
        }
    }
}