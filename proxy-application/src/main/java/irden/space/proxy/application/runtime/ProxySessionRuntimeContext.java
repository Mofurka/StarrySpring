package irden.space.proxy.application.runtime;


import irden.space.proxy.domain.session.ProxySession;

import java.net.Socket;

public record ProxySessionRuntimeContext(ProxySession session,
                                         Socket clientSocket,
                                         Socket upstreamSocket,
                                         SwitchableSessionTransport clientSideTransport,
                                         SwitchableSessionTransport upstreamSideTransport
) {

}
