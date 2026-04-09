package irden.space.proxy.application.port.out;

import irden.space.proxy.domain.session.ProxySession;
import irden.space.proxy.domain.session.ProxySessionId;

import java.util.Collection;
import java.util.Optional;

public interface SessionRegistry {
    void add(ProxySession session);
    void remove(ProxySessionId sessionId);
    Optional<ProxySession> getById(ProxySessionId sessionId);
    Collection<ProxySession> getAllSessions();
}
