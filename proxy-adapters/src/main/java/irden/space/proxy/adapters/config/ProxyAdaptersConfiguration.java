package irden.space.proxy.adapters.config;

import irden.space.proxy.adapters.session.InMemorySessionRegistry;
import irden.space.proxy.application.port.out.SessionRegistry;

public final class ProxyAdaptersConfiguration {

    private ProxyAdaptersConfiguration() {
    }

    public static SessionRegistry sessionRegistry() {
        return new InMemorySessionRegistry();
    }
}
