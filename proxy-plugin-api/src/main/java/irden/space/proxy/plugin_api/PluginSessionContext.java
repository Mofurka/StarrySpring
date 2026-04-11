package irden.space.proxy.plugin_api;


public interface PluginSessionContext {

    String sessionId();

    String clientIp();

    boolean clientZstdEnabled();

    boolean upstreamZstdEnabled();
}
