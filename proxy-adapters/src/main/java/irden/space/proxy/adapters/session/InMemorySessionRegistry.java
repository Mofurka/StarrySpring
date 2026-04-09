package irden.space.proxy.adapters.session;

import irden.space.proxy.application.port.out.SessionRegistry;
import irden.space.proxy.domain.session.ProxySession;
import irden.space.proxy.domain.session.ProxySessionId;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemorySessionRegistry implements SessionRegistry {
    private final ConcurrentMap<ProxySessionId, ProxySession> sessions = new ConcurrentHashMap<>();

    @Override
    public void add(ProxySession session) {
        sessions.put(session.getId(), session);
    }

    @Override
    public void remove(ProxySessionId sessionId) {
        sessions.remove(sessionId);
    }

    @Override
    public Optional<ProxySession> getById(ProxySessionId sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    @Override
    public Collection<ProxySession> getAllSessions() {
        return List.copyOf(sessions.values());
    }
}
