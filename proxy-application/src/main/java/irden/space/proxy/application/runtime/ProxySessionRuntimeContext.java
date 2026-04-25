package irden.space.proxy.application.runtime;


import irden.space.proxy.domain.session.ProxySession;
import irden.space.proxy.plugin.api.SessionPermissionService;

import java.net.Socket;

public record ProxySessionRuntimeContext(ProxySession session,
                                         Socket clientSocket,
                                         Socket upstreamSocket,
                                         SwitchableSessionTransport clientSideTransport,
                                         SwitchableSessionTransport upstreamSideTransport,
                                         SessionPermissionService sessionPermissionService
) {

}
