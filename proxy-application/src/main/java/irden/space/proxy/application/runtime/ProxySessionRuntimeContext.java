package irden.space.proxy.application.runtime;


import irden.space.proxy.domain.session.ProxySession;

import java.net.Socket;

public class ProxySessionRuntimeContext {

    private final ProxySession session;
    private final Socket clientSocket;
    private final Socket upstreamSocket;

    private final SwitchableSessionTransport clientSideTransport;
    private final SwitchableSessionTransport upstreamSideTransport;

    public ProxySessionRuntimeContext(
            ProxySession session,
            Socket clientSocket,
            Socket upstreamSocket,
            SwitchableSessionTransport clientSideTransport,
            SwitchableSessionTransport upstreamSideTransport
    ) {
        this.session = session;
        this.clientSocket = clientSocket;
        this.upstreamSocket = upstreamSocket;
        this.clientSideTransport = clientSideTransport;
        this.upstreamSideTransport = upstreamSideTransport;
    }

    public ProxySession session() {
        return session;
    }

    public void switchToZstd() {
        synchronized (session) {
            if (clientSideTransport.isZstd() || upstreamSideTransport.isZstd()) {
                throw new IllegalStateException("Already switched to zstd");
            }
            clientSideTransport.switchToZstd();
            upstreamSideTransport.switchToZstd();
            session.enableClientZstd();
            session.enableUpstreamZstd();
        }
    }

    public Socket clientSocket() {
        return clientSocket;
    }

    public Socket upstreamSocket() {
        return upstreamSocket;
    }

    public SwitchableSessionTransport clientSideTransport() {
        return clientSideTransport;
    }

    public SwitchableSessionTransport upstreamSideTransport() {
        return upstreamSideTransport;
    }
}
