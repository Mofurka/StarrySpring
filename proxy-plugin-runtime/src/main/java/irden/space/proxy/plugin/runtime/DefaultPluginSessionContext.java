package irden.space.proxy.plugin.runtime;

import irden.space.proxy.plugin.api.PluginSessionContext;

public class DefaultPluginSessionContext implements PluginSessionContext {

    private final String sessionId;
    private final String clientIp;
    private final boolean clientZstdEnabled;
    private final boolean upstreamZstdEnabled;

    public DefaultPluginSessionContext(
            String sessionId,
            String clientIp,
            boolean clientZstdEnabled,
            boolean upstreamZstdEnabled
    ) {
        this.sessionId = sessionId;
        this.clientIp = clientIp;
        this.clientZstdEnabled = clientZstdEnabled;
        this.upstreamZstdEnabled = upstreamZstdEnabled;
    }

    @Override
    public String sessionId() {
        return sessionId;
    }

    @Override
    public String clientIp() {
        return clientIp;
    }

    @Override
    public boolean clientZstdEnabled() {
        return clientZstdEnabled;
    }

    @Override
    public boolean upstreamZstdEnabled() {
        return upstreamZstdEnabled;
    }
}

