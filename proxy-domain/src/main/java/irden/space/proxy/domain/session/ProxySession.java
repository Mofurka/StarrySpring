package irden.space.proxy.domain.session;

import lombok.Getter;
import lombok.ToString;

import java.util.Objects;

@Getter
@ToString
public final class ProxySession {
    private final ProxySessionId id;
    private final String clientIp;
    private volatile SessionState state;
    private volatile SessionTransportMode clientCompression;
    private volatile SessionTransportMode upstreamCompression;
    private volatile Integer openProtocolVersion;


    public ProxySession(ProxySessionId id, String clientIp) {
        this.id = Objects.requireNonNull(id, "Session ID cannot be null");
        this.clientIp = Objects.requireNonNull(clientIp, "Client IP cannot be null");
        this.state = SessionState.NEW;
        this.clientCompression = SessionTransportMode.PLAIN;
        this.upstreamCompression = SessionTransportMode.PLAIN;
        this.openProtocolVersion = null;
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
        setClientTransportMode(SessionTransportMode.ZSTD);
    }

    public void enableUpstreamZstd() {
        setUpstreamTransportMode(SessionTransportMode.ZSTD);
    }

    public void setClientTransportMode(SessionTransportMode transportMode) {
        this.clientCompression = Objects.requireNonNull(transportMode, "Client transport mode cannot be null");
    }

    public void setUpstreamTransportMode(SessionTransportMode transportMode) {
        this.upstreamCompression = Objects.requireNonNull(transportMode, "Upstream transport mode cannot be null");
    }

    public void setOpenProtocolVersion(int openProtocolVersion) {
        this.openProtocolVersion = openProtocolVersion;
    }

    public int resolveOpenProtocolVersion() {
        return openProtocolVersion != null
                ? openProtocolVersion
                : -1;
    }

    private void ensureNotDisconnected() {
        if (this.state == SessionState.DISCONNECTED) {
            throw new IllegalStateException("Session is already disconnected");
        }
    }

}
