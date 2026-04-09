package irden.space.proxy.domain.session;


public enum SessionState {
    DISCONNECTED,
    DISCONNECTING,
    NEW,
    UPSTREAM_CONNECTING,
    ACTIVE,
}
