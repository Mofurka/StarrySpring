package irden.space.proxy.application.runtime;

import irden.space.proxy.application.port.out.SessionRegistry;
import irden.space.proxy.domain.session.ProxySession;
import irden.space.proxy.domain.session.SessionState;
import irden.space.proxy.domain.session.SessionTransportMode;
import irden.space.proxy.plugin.api.*;
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
    private static final PacketInspectionResult EMPTY_INSPECTION = PacketInspectionResult.empty();

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
    private final PluginSessionLifecycleService pluginSessionLifecycleService;

    public PacketForwarder(
            InputStream source,
            OutputStream target,
            SessionRegistry sessionRegistry,
            PacketDirection packetDirection,
            ProxySessionRuntimeContext context,
            SwitchableSessionTransport transport,
            RuntimePacketInspector packetInspector,
            PacketInterceptionService packetInterceptionService,
            PluginSessionLifecycleService pluginSessionLifecycleService
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
        this.pluginSessionLifecycleService = pluginSessionLifecycleService;
    }

    @Override
    public void run() {
        try {
            while (!clientSocket.isClosed() && !upstreamSocket.isClosed()) {
                PacketEnvelope envelope = readPacket();
                if (envelope == null) {
                    continue;
                }

                int openProtocolVersion = session.resolveOpenProtocolVersion();
                PacketInspectionResult inspection = inspectPacket(envelope, packetDirection, openProtocolVersion);

                int resolvedOpenProtocolVersion = inspection.negotiatedOpenProtocolVersion() != null
                        ? inspection.negotiatedOpenProtocolVersion()
                        : openProtocolVersion;

/*                log.debug(
                        "[{}] session={} rawType={} type={} size={} compressed={} parsed={}",
                        packetDirection,
                        session.getId(),
                        envelope.rawPacketTypeId(),
                        envelope.packetType(),
                        envelope.payloadSize(),
                        envelope.compressed(),
                        inspection.parsed()
                );*/

                PluginSessionContext pluginSessionContext = createPluginSessionContext(resolvedOpenProtocolVersion);

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

                writePacket(packetDirection, envelopeToWrite, envelopeToWrite == envelope ? inspection : null);
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

    private void sendPacket(PacketDirection direction, PacketEnvelope envelope) {
        try {
            writePacket(direction, envelope, null);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to send packet for session " + session.getId(), e);
        }
    }

    private void writePacket(
            PacketDirection direction,
            PacketEnvelope envelope,
            PacketInspectionResult inspection
    ) throws IOException {
        SwitchableSessionTransport resolvedTransport = resolveTransport(direction);
        OutputStream resolvedTarget = resolveTarget(direction);

        synchronized (resolveWriteLock(direction)) {
            PacketInspectionResult resolvedInspection = inspection;
            if (resolvedInspection == null) {
                resolvedInspection = inspectPacket(envelope, direction, session.resolveOpenProtocolVersion());
            }

            applyNegotiatedSessionState(resolvedInspection);

            resolvedTransport.write(resolvedTarget, envelope);
        }
    }

    private PacketInspectionResult inspectPacket(
            PacketEnvelope envelope,
            PacketDirection direction,
            int openProtocolVersion
    ) {
        if (packetInspector == null) {
            return EMPTY_INSPECTION;
        }

        return packetInspector.inspect(envelope, direction, openProtocolVersion);
    }

    private Object resolveWriteLock(PacketDirection direction) {
        return direction == PacketDirection.TO_CLIENT
                ? context.clientSocket()
                : context.upstreamSocket();
    }

    private SwitchableSessionTransport resolveTransport(PacketDirection direction) {
        return direction == PacketDirection.TO_CLIENT
                ? context.upstreamSideTransport()
                : context.clientSideTransport();
    }

    private OutputStream resolveTarget(PacketDirection direction) throws IOException {
        if (direction == packetDirection) {
            return target;
        }

        return direction == PacketDirection.TO_CLIENT
                ? context.clientSocket().getOutputStream()
                : context.upstreamSocket().getOutputStream();
    }
    private void applyNegotiatedSessionState(PacketInspectionResult inspection) {
        if (inspection.negotiatedOpenProtocolVersion() != null) {
            session.setOpenProtocolVersion(inspection.negotiatedOpenProtocolVersion());
        }

        if (inspection.negotiatedTransportMode() != null) {
            log.info(
                    "Session {} negotiated transport mode {} via ProtocolResponse",
                    session.getId(),
                    inspection.negotiatedTransportMode()
            );
            switchSessionTransportMode(inspection.negotiatedTransportMode());
        }
    }

    private void switchSessionTransportMode(SessionTransportMode transportMode) {
        synchronized (context.session()) {
            if (context.clientSideTransport().isReadModeEnabled(transportMode)
                    && context.clientSideTransport().isWriteModeEnabled(transportMode)
                    && context.upstreamSideTransport().isReadModeEnabled(transportMode)
                    && context.upstreamSideTransport().isWriteModeEnabled(transportMode)) {
                return;
            }

            if (transportMode == SessionTransportMode.PLAIN) {
                context.clientSideTransport().enableReadMode(SessionTransportMode.PLAIN);
                context.upstreamSideTransport().enableReadMode(SessionTransportMode.PLAIN);
                context.clientSideTransport().enableWriteMode(SessionTransportMode.PLAIN, 0);
                context.upstreamSideTransport().enableWriteMode(SessionTransportMode.PLAIN, 0);
                context.session().setClientTransportMode(SessionTransportMode.PLAIN);
                context.session().setUpstreamTransportMode(SessionTransportMode.PLAIN);
                log.info("Session {} switched to {} transport mode", context.session().getId(), transportMode);
                return;
            }

            context.clientSideTransport().enableReadMode(transportMode);
            context.upstreamSideTransport().enableReadMode(transportMode);

            waitForPlainReadersToDrain();


            context.clientSideTransport().enableWriteMode(transportMode, 0);
            context.upstreamSideTransport().enableWriteMode(transportMode, 1);

            context.session().setClientTransportMode(transportMode);
            context.session().setUpstreamTransportMode(transportMode);

            log.info("Session {} switched to {} transport mode", context.session().getId(), transportMode);
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

            PluginSessionContext pluginSessionContext = createPluginSessionContext(session.resolveOpenProtocolVersion());

            try {
                session.markDisconnecting();
            } catch (Exception e) {
                log.warn("Failed to mark session {} as DISCONNECTING", session.getId());
            }

            try {
                pluginSessionLifecycleService.onDisconnecting(pluginSessionContext);
            } catch (Exception e) {
                log.warn("Failed to dispatch OnDisconnecting for session {}", session.getId(), e);
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

            try {
                pluginSessionLifecycleService.onDisconnected(pluginSessionContext);
            } catch (Exception e) {
                log.warn("Failed to dispatch OnDisconnected for session {}", session.getId(), e);
            }

            log.info("Session {} closed and removed", session.getId());
        }
    }

    private PluginSessionContext createPluginSessionContext(int openProtocolVersion) {
        return new DefaultPluginSessionContext(
                session.getId().uuid().toString(),
                session.getClientIp(),
                session.getClientCompression() == SessionTransportMode.ZSTD,
                session.getUpstreamCompression() == SessionTransportMode.ZSTD,
                openProtocolVersion,
                this::sendPacket
        );
    }
}
