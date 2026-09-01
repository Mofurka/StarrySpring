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


    private static final int PLAIN_PACKETS_AFTER_SWITCH = 1;

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
    private final String sessionId;
    private final PermissionView permissionView;
    private final Runnable onClosed;
    private final long idleTimeoutMillis;

    private volatile PluginSessionContext cachedPluginSessionContext;
    private volatile int cachedOpenProtocolVersion = Integer.MIN_VALUE;

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
        this(source, target, sessionRegistry, packetDirection, context, transport,
                packetInspector, packetInterceptionService, pluginSessionLifecycleService, () -> {
                });
    }

    public PacketForwarder(
            InputStream source,
            OutputStream target,
            SessionRegistry sessionRegistry,
            PacketDirection packetDirection,
            ProxySessionRuntimeContext context,
            SwitchableSessionTransport transport,
            RuntimePacketInspector packetInspector,
            PacketInterceptionService packetInterceptionService,
            PluginSessionLifecycleService pluginSessionLifecycleService,
            Runnable onClosed
    ) {
        this(source, target, sessionRegistry, packetDirection, context, transport,
                packetInspector, packetInterceptionService, pluginSessionLifecycleService, onClosed, 0L);
    }

    public PacketForwarder(
            InputStream source,
            OutputStream target,
            SessionRegistry sessionRegistry,
            PacketDirection packetDirection,
            ProxySessionRuntimeContext context,
            SwitchableSessionTransport transport,
            RuntimePacketInspector packetInspector,
            PacketInterceptionService packetInterceptionService,
            PluginSessionLifecycleService pluginSessionLifecycleService,
            Runnable onClosed,
            long idleTimeoutMillis
    ) {
        this.idleTimeoutMillis = idleTimeoutMillis;
        this.onClosed = onClosed;
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
        this.sessionId = session.getId().uuid().toString();
        this.permissionView = permissionId -> context.sessionPermissionService()
                .permissions(sessionId)
                .has(permissionId);
    }

    @Override
    public void run() {
        try {
            while (!clientSocket.isClosed() && (upstreamSocket == null || !upstreamSocket.isClosed())) {
                PacketEnvelope envelope = readPacket();
                if (envelope == null) {
                    if (isSessionStalled()) {
                        break;
                    }
                    continue;
                }

                session.recordActivity();

                int openProtocolVersion = session.resolveOpenProtocolVersion();
                PacketInspectionResult inspection = inspectPacket(envelope, packetDirection, openProtocolVersion);

                int resolvedOpenProtocolVersion = inspection.negotiatedOpenProtocolVersion() != null
                        ? inspection.negotiatedOpenProtocolVersion()
                        : openProtocolVersion;

                PluginSessionContext pluginSessionContext = createPluginSessionContext(resolvedOpenProtocolVersion);

                PacketInterceptionContext interceptionContext =
                        PacketInterceptionContext.lazy(
                                pluginSessionContext,
                                envelope,
                                inspection.parsedPayloadSupplier(),
                                packetDirection
                        );

                PacketDecision decision = packetInterceptionService.apply(interceptionContext);

                if (decision instanceof DropPacketDecision(Runnable afterDrop)) {
                    runAfterAction(packetDirection, "after-drop", afterDrop);
                    continue;
                }

                if (packetDirection == PacketDirection.TO_SERVER && upstreamSocket == null) {
                    log.warn(
                            "[{}] no upstream connection for session {} and packet was not handled by plugins, closing session",
                            packetDirection,
                            session.getId()
                    );
                    break;
                }

                PacketEnvelope envelopeToWrite = envelope;
                Runnable afterWrite = null;

                if (decision instanceof ReplacePacketDecision(PacketEnvelope replacement, Runnable afterReplace)) {
                    envelopeToWrite = replacement;
                    afterWrite = afterReplace;
                } else if (decision instanceof ForwardPacketDecision(Runnable afterForward)) {
                    afterWrite = afterForward;
                }

                writePacket(
                        packetDirection,
                        envelopeToWrite,
                        envelopeToWrite == envelope ? inspection : null,
                        afterWrite
                );
            }
        } catch (SocketException e) {
            log.info("[{}] socket exception for session {}: {}", packetDirection, session.getId(), e.getMessage());
        } catch (Exception e) {
            log.warn("[{}] forwarding stopped for session {}: {}", packetDirection, session.getId(), e.getMessage(), e);
        } finally {
            closeSession();
        }
    }


    private boolean isSessionStalled() {
        if (idleTimeoutMillis <= 0 || upstreamSocket == null) {
            return false;
        }

        long idleMillis = session.idleMillis();
        if (idleMillis < idleTimeoutMillis) {
            return false;
        }

        log.warn(
                "[{}] session {} is silent in both directions for {} ms, closing it",
                packetDirection,
                session.getId(),
                idleMillis
        );

        context.closeSockets();
        return true;
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
            writePacket(direction, envelope);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to send packet for session " + session.getId(), e);
        }
    }

    private void writePacket(
            PacketDirection direction,
            PacketEnvelope envelope
    ) throws IOException {
        SwitchableSessionTransport resolvedTransport = resolveTransport(direction);
        OutputStream resolvedTarget = resolveTarget(direction);

        synchronized (resolveWriteLock(direction)) {
            resolvedTransport.write(resolvedTarget, envelope);
        }
    }


    private void writePacket(
            PacketDirection direction,
            PacketEnvelope envelope,
            PacketInspectionResult inspection,
            Runnable afterWrite
    ) throws IOException {
        SwitchableSessionTransport resolvedTransport =
                resolveTransport(direction);

        OutputStream resolvedTarget =
                resolveTarget(direction);

        synchronized (resolveWriteLock(direction)) {
            PacketInspectionResult resolvedInspection = inspection;

            if (resolvedInspection == null) {
                resolvedInspection = inspectPacket(
                        envelope,
                        direction,
                        session.resolveOpenProtocolVersion()
                );
            }

            applyNegotiatedSessionState(resolvedInspection);

            resolvedTransport.write(resolvedTarget, envelope);

            runAfterAction(direction, "after-forward", afterWrite);
        }
    }

    private void runAfterAction(PacketDirection direction, String kind, Runnable afterAction) {
        if (afterAction == null) {
            return;
        }
        try {
            afterAction.run();
        } catch (Exception e) {
            log.warn("[{}] {} callback failed for session {}", direction, kind, session.getId(), e);
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
        if (direction == PacketDirection.TO_CLIENT) {
            return context.clientSocket();
        }
        Socket upstream = context.upstreamSocket();
        return upstream != null ? upstream : session;
    }

    private SwitchableSessionTransport resolveTransport(PacketDirection direction) {
        return direction == PacketDirection.TO_CLIENT
                ? context.upstreamSideTransport()
                : context.clientSideTransport();
    }

    private OutputStream resolveTarget(PacketDirection direction) throws IOException {
        if (direction == packetDirection && target != null) {
            return target;
        }

        if (direction == PacketDirection.TO_CLIENT) {
            return context.clientSocket().getOutputStream();
        }

        Socket upstream = context.upstreamSocket();
        if (upstream == null) {
            throw new IOException("No upstream connection for session " + session.getId());
        }
        return upstream.getOutputStream();
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

            awaitPeerReaderSwitch(transportMode);

            context.clientSideTransport().enableWriteMode(transportMode, 0);
            context.upstreamSideTransport().enableWriteMode(transportMode, PLAIN_PACKETS_AFTER_SWITCH);

            context.session().setClientTransportMode(transportMode);
            context.session().setUpstreamTransportMode(transportMode);

            log.info("Session {} switched to {} transport mode", context.session().getId(), transportMode);
        }
    }


    private void awaitPeerReaderSwitch(SessionTransportMode transportMode) {
        SwitchableSessionTransport peerTransport = peerReadTransport();
        long timeoutMillis = resolveReadSwitchTimeoutMillis();

        try {
            if (peerTransport.awaitReadModeApplied(transportMode, timeoutMillis)) {
                return;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while switching session transport to " + transportMode, e);
        }

        log.warn(
                "[{}] peer reader of session {} did not confirm the switch to {} within {} ms;"
                        + " proceeding anyway, the first compressed packet may be lost",
                packetDirection,
                session.getId(),
                transportMode,
                timeoutMillis
        );
    }


    private SwitchableSessionTransport peerReadTransport() {
        return transport == context.clientSideTransport()
                ? context.upstreamSideTransport()
                : context.clientSideTransport();
    }


    private long resolveReadSwitchTimeoutMillis() {
        int clientTimeout = readSocketTimeout(context.clientSocket());
        int upstreamTimeout = readSocketTimeout(context.upstreamSocket());
        int maxTimeout = Math.max(clientTimeout, upstreamTimeout);

        if (maxTimeout <= 0) {
            return 100L;
        }

        return maxTimeout * 2L + 100L;
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

            if (upstreamSocket != null) {
                try {
                    upstreamSocket.close();
                } catch (Exception e) {
                    log.warn("Failed to close upstream socket for session {}", session.getId());
                }
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

            try {
                onClosed.run();
            } catch (Exception e) {
                log.debug("Session close callback failed for session {}", session.getId(), e);
            }
        }
    }

    private PluginSessionContext createPluginSessionContext(int openProtocolVersion) {
        PluginSessionContext cachedContext = cachedPluginSessionContext;
        if (cachedContext != null && cachedOpenProtocolVersion == openProtocolVersion) {
            return cachedContext;
        }

        synchronized (this) {
            cachedContext = cachedPluginSessionContext;
            if (cachedContext != null && cachedOpenProtocolVersion == openProtocolVersion) {
                return cachedContext;
            }

            PluginSessionContext newContext = new DefaultPluginSessionContext(
                    sessionId,
                    session.getClientIp(),
                    session.getClientCompression() == SessionTransportMode.ZSTD,
                    session.getUpstreamCompression() == SessionTransportMode.ZSTD,
                    openProtocolVersion,
                    this::sendPacket,
                    permissionView,
                    context::closeSockets,
                    context.pluginAttributes()
            );
            cachedOpenProtocolVersion = openProtocolVersion;
            cachedPluginSessionContext = newContext;
            return newContext;
        }
    }
}
