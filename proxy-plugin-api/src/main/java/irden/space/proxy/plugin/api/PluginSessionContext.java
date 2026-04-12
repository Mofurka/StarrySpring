package irden.space.proxy.plugin.api;


public interface PluginSessionContext {

    String sessionId();

    String clientIp();

    boolean clientZstdEnabled();

    boolean upstreamZstdEnabled();
}
