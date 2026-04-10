package irden.space.proxy.domain.session;

import lombok.Getter;
import lombok.ToString;

import java.util.Objects;

@Getter
@ToString
public final class ProxySession {
    private final ProxySessionId id;
    private final String clientIp;
    private SessionState state;
    private SessionTransportMode clientCompression;
    private SessionTransportMode upstreamCompression;


    public ProxySession(ProxySessionId id, String clientIp) {
        this.id = Objects.requireNonNull(id, "Session ID cannot be null");
        this.clientIp = Objects.requireNonNull(clientIp, "Client IP cannot be null");
        this.state = SessionState.NEW;
        this.clientCompression = SessionTransportMode.PLAIN;
        this.upstreamCompression = SessionTransportMode.PLAIN;
    }

    public void makeUpstreamConnecting() {
        ensureNotDisconnected();
        this.state = SessionState.UPSTREAM_CONNECTING;
    }

    public void activate() {
        ensureNotDisconnected();
        this.state = SessionState.ACTIVE;
    }


    public void markDisconnecting() {
        ensureNotDisconnected();
        this.state = SessionState.DISCONNECTING;
    }

    public void markDisconnected() {
        this.state = SessionState.DISCONNECTED;
    }

    public void enableClientZstd() {
        this.clientCompression = SessionTransportMode.ZSTD;
    }

    public void enableUpstreamZstd() {
        this.upstreamCompression = SessionTransportMode.ZSTD;
    }

    private void ensureNotDisconnected() {
        if (this.state == SessionState.DISCONNECTED) {
            throw new IllegalStateException("Session is already disconnected");
        }
    }

}
