package irden.space.proxy.plugin.api;

public interface PluginSessionLifecycleService {

    void onConnectionSuccess(PluginSessionContext context);

    void onDisconnecting(PluginSessionContext context);

    void onDisconnected(PluginSessionContext context);
}

