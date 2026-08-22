package irden.space.proxy.application.runtime;


import irden.space.proxy.domain.session.ProxySession;
import irden.space.proxy.plugin.api.SessionPermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Socket;


public record ProxySessionRuntimeContext(ProxySession session,
                                         Socket clientSocket,
                                         Socket upstreamSocket,
                                         SwitchableSessionTransport clientSideTransport,
                                         SwitchableSessionTransport upstreamSideTransport,
                                         SessionPermissionService sessionPermissionService
) {

    private static final Logger log = LoggerFactory.getLogger(ProxySessionRuntimeContext.class);

    public void closeSockets() {
        closeQuietly(clientSocket, "client");
        closeQuietly(upstreamSocket, "upstream");
    }

    private void closeQuietly(Socket socket, String side) {
        if (socket == null || socket.isClosed()) {
            return;
        }

        try {
            socket.close();
        } catch (Exception e) {
            log.warn("Failed to close {} socket for session {}: {}", side, session.getId(), e.getMessage());
        }
    }
}
