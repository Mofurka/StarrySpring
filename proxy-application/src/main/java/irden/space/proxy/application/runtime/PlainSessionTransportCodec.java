package irden.space.proxy.application.runtime;

import irden.space.proxy.domain.session.SessionTransportMode;

public final class PlainSessionTransportCodec implements SessionTransportCodec {

    @Override
    public SessionTransportMode mode() {
        return SessionTransportMode.PLAIN;
    }
}

