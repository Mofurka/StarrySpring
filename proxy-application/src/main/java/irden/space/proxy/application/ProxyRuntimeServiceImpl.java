package irden.space.proxy.application;

import irden.space.proxy.application.port.out.SessionRegistry;
import irden.space.proxy.application.runtime.PacketForwarder;
import irden.space.proxy.application.runtime.ProxySessionRuntimeContext;
import irden.space.proxy.application.runtime.RuntimePacketInspector;
import irden.space.proxy.application.runtime.SwitchableSessionTransport;
import irden.space.proxy.domain.session.ProxySession;
import irden.space.proxy.domain.session.ProxySessionId;
import irden.space.proxy.domain.session.SessionTransportMode;
import irden.space.proxy.plugin.api.PacketInterceptionService;
import irden.space.proxy.protocol.packet.PacketDirection;
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
    private static final int SESSION_SOCKET_TIMEOUT_MILLIS = 200;

    @Override
    public void start() {
        log.info("Starting proxy runtime: {}", properties);

        try (ServerSocket serverSocket = new ServerSocket(properties.getListenPort())) {
            log.info("Proxy listening on {}:{}", properties.getListenHost(), properties.getListenPort());

            while (true) {
                Socket clientSocket = serverSocket.accept();
                handleNewClient(clientSocket);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to start proxy runtime", e);
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

            upstreamSocket = new Socket(
                    properties.getUpstreamHost(),
                    properties.getUpstreamPort()
            );
            configureSocket(upstreamSocket);

            session.makeUpstreamConnecting();
            log.info(
                    "Connected upstream {}:{} for session {}",
                    properties.getUpstreamHost(),
                    properties.getUpstreamPort(),
                    session.getId()
            );

            InputStream clientIn = clientSocket.getInputStream();
            OutputStream clientOut = clientSocket.getOutputStream();

            InputStream upstreamIn = upstreamSocket.getInputStream();
            OutputStream upstreamOut = upstreamSocket.getOutputStream();

            session.activate();
            log.info("Session {} is ACTIVE", session.getId());

            ProxySessionRuntimeContext context = new ProxySessionRuntimeContext(
                    session,
                    clientSocket,
                    upstreamSocket,
                    new SwitchableSessionTransport(SessionTransportMode.PLAIN),
                    new SwitchableSessionTransport(SessionTransportMode.PLAIN)
            );

            Thread clientToServer = new Thread(
                    new PacketForwarder(
                            clientIn,
                            upstreamOut,
                            sessionRegistry,
                            PacketDirection.TO_SERVER,
                            context,
                            context.clientSideTransport(),
                            packetInspector,
                            packetInterceptionService
                    ),
                    session.getId().uuid() + "-proxy-c2s"
            );

            Thread serverToClient = new Thread(
                    new PacketForwarder(
                            upstreamIn,
                            clientOut,
                            sessionRegistry,
                            PacketDirection.TO_CLIENT,
                            context,
                            context.upstreamSideTransport(),
                            packetInspector,
                            packetInterceptionService
                    ),
                    session.getId().uuid() + "-proxy-s2c"
            );

            clientToServer.start();
            serverToClient.start();

        } catch (Exception e) {
            log.warn("Failed to initialize client connection: {}", e.getMessage(), e);
            closeQuietly(upstreamSocket, "upstream", clientSocket);
            closeQuietly(clientSocket, "client", clientSocket);
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
}
