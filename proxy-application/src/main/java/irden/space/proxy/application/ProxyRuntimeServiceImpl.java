package irden.space.proxy.application;

import irden.space.proxy.application.port.out.SessionRegistry;
import irden.space.proxy.application.runtime.PacketForwarder;
import irden.space.proxy.application.runtime.ProxySessionRuntimeContext;
import irden.space.proxy.application.runtime.RuntimePacketInspector;
import irden.space.proxy.application.runtime.SwitchableSessionTransport;
import irden.space.proxy.domain.session.ProxySession;
import irden.space.proxy.domain.session.ProxySessionId;
import irden.space.proxy.domain.session.SessionTransportMode;
import irden.space.proxy.plugin.api.*;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketEnvelope;
import irden.space.proxy.protocol.payload.registry.PacketDispatcher;
import irden.space.proxy.protocol.payload.registry.PacketParserRegistry;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

@Service
@RequiredArgsConstructor
public class ProxyRuntimeServiceImpl implements ProxyRuntimeService {

    private static final Logger log = LoggerFactory.getLogger(ProxyRuntimeServiceImpl.class);

    private final SessionRegistry sessionRegistry;
    private final ProxyServerProperties properties;
    private final PacketParserRegistry packetParserRegistry = new PacketParserRegistry();
    private final PacketDispatcher packetDispatcher = new PacketDispatcher(packetParserRegistry);
    private final RuntimePacketInspector packetInspector = new RuntimePacketInspector(packetDispatcher);
    private final PacketInterceptionService packetInterceptionService;
    private final PluginSessionLifecycleService pluginSessionLifecycleService;
    private final SessionPermissionService sessionPermissionService;
    private static final int SESSION_SOCKET_TIMEOUT_MILLIS = 200;

    private volatile boolean running;
    private ServerSocket serverSocket;
    private Thread acceptThread;

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }

        log.info("Starting proxy runtime: {}", properties);

        try {
            serverSocket = new ServerSocket(properties.getListenPort());
            running = true;

            acceptThread = new Thread(this::acceptLoop, "proxy-accept-loop");
            acceptThread.setDaemon(false);
            acceptThread.start();

            log.info("Proxy listening on {}:{}", properties.getListenHost(), properties.getListenPort());
        } catch (Exception e) {
            running = false;
            closeServerSocketQuietly();
            throw new IllegalStateException("Failed to start proxy runtime", e);
        }
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                handleNewClient(clientSocket);
            } catch (SocketException e) {
                if (running) {
                    log.warn("Proxy accept loop socket error", e);
                }
                break;
            } catch (Exception e) {
                if (running) {
                    log.warn("Failed to accept client connection", e);
                }
            }
        }

        log.info("Proxy accept loop stopped");
    }

    @Override
    public synchronized void stop() {
        if (!running) {
            return;
        }

        log.info("Stopping proxy runtime...");
        running = false;
        closeServerSocketQuietly();

        if (acceptThread != null) {
            acceptThread.interrupt();
        }
    }

    private void closeServerSocketQuietly() {
        if (serverSocket == null) {
            return;
        }

        try {
            serverSocket.close();
        } catch (Exception e) {
            log.debug("Failed to close proxy server socket", e);
        }
    }

    private void handleNewClient(Socket clientSocket) {
        Socket upstreamSocket = null;
        try {
            configureSocket(clientSocket);

            ProxySession session = new ProxySession(
                    ProxySessionId.generate(),
                    clientSocket.getInetAddress().getHostAddress()
            );

            sessionRegistry.add(session);
            log.info("Accepted client {} for session {}", session.getClientIp(), session.getId());

            session.makeUpstreamConnecting();
            upstreamSocket = connectUpstream(session);

            InputStream clientIn = clientSocket.getInputStream();
            OutputStream clientOut = clientSocket.getOutputStream();

            InputStream upstreamIn = upstreamSocket != null ? upstreamSocket.getInputStream() : null;
            OutputStream upstreamOut = upstreamSocket != null ? upstreamSocket.getOutputStream() : null;

            session.activate();
            log.info("Session {} is ACTIVE", session.getId());

            ProxySessionRuntimeContext context = new ProxySessionRuntimeContext(
                    session,
                    clientSocket,
                    upstreamSocket,
                    new SwitchableSessionTransport(SessionTransportMode.PLAIN),
                    new SwitchableSessionTransport(SessionTransportMode.PLAIN),
                    sessionPermissionService
            );

            pluginSessionLifecycleService.onConnectionSuccess(createPluginSessionContext(context));

            Thread clientToServer = new Thread(
                    new PacketForwarder(
                            clientIn,
                            upstreamOut,
                            sessionRegistry,
                            PacketDirection.TO_SERVER,
                            context,
                            context.clientSideTransport(),
                            packetInspector,
                            packetInterceptionService,
                            pluginSessionLifecycleService
                    ),
                    session.getId().uuid() + "-proxy-c2s"
            );

            clientToServer.start();

            if (upstreamIn != null) {
                Thread serverToClient = new Thread(
                        new PacketForwarder(
                                upstreamIn,
                                clientOut,
                                sessionRegistry,
                                PacketDirection.TO_CLIENT,
                                context,
                                context.upstreamSideTransport(),
                                packetInspector,
                                packetInterceptionService,
                                pluginSessionLifecycleService
                        ),
                        session.getId().uuid() + "-proxy-s2c"
                );
                serverToClient.start();
            }

        } catch (Exception e) {
            log.warn("Failed to initialize client connection: {}", e.getMessage(), e);
            closeQuietly(upstreamSocket, "upstream", clientSocket);
            closeQuietly(clientSocket, "client", clientSocket);
        }
    }


    private Socket connectUpstream(ProxySession session) {
        try {
            Socket upstreamSocket = new Socket(
                    properties.getUpstreamHost(),
                    properties.getUpstreamPort()
            );
            configureSocket(upstreamSocket);
            log.info(
                    "Connected upstream {}:{} for session {}",
                    properties.getUpstreamHost(),
                    properties.getUpstreamPort(),
                    session.getId()
            );
            return upstreamSocket;
        } catch (Exception e) {
            log.warn(
                    "Upstream {}:{} is unavailable for session {} ({}), continuing without it",
                    properties.getUpstreamHost(),
                    properties.getUpstreamPort(),
                    session.getId(),
                    e.getMessage()
            );
            return null;
        }
    }

    private void configureSocket(Socket socket) throws SocketException {
        socket.setSoTimeout(SESSION_SOCKET_TIMEOUT_MILLIS);
    }

    private void closeQuietly(Socket socket, String side, Socket clientSocket) {
        if (socket == null) {
            return;
        }

        try {
            socket.close();
        } catch (Exception e) {
            log.debug("Failed to close {} socket after initialization error for client {}", side, clientSocket, e);
        }
    }

    private DefaultPluginSessionContext createPluginSessionContext(ProxySessionRuntimeContext context) {
        ProxySession session = context.session();
        PermissionView permissionView = permissionId -> context.sessionPermissionService()
                .permissions(session.getId().uuid().toString())
                .has(permissionId);
        return new DefaultPluginSessionContext(
                session.getId().uuid().toString(),
                session.getClientIp(),
                session.getClientCompression() == SessionTransportMode.ZSTD,
                session.getUpstreamCompression() == SessionTransportMode.ZSTD,
                session.resolveOpenProtocolVersion(),
                (direction, envelope) -> sendPacket(context, direction, envelope),
                permissionView
        );
    }

    private void sendPacket(ProxySessionRuntimeContext context, PacketDirection direction, PacketEnvelope envelope) {
        if (direction == PacketDirection.TO_SERVER && context.upstreamSocket() == null) {
            throw new IllegalStateException(
                    "No upstream connection for session " + context.session().getId() + ", cannot send packet to server"
            );
        }
        try {
            synchronized (direction == PacketDirection.TO_CLIENT ? context.clientSocket() : context.upstreamSocket()) {
                SwitchableSessionTransport transport = direction == PacketDirection.TO_CLIENT
                        ? context.upstreamSideTransport()
                        : context.clientSideTransport();
                OutputStream outputStream = direction == PacketDirection.TO_CLIENT
                        ? context.clientSocket().getOutputStream()
                        : context.upstreamSocket().getOutputStream();
                transport.write(outputStream, envelope);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to send lifecycle packet for session " + context.session().getId(), e);
        }
    }
}
