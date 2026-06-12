package irden.space.proxy.plugin.api;

import java.util.List;

public interface PluginRuntimeService {

    List<PluginRuntimeView> plugins();

    List<String> startPlugin(String pluginId);

    List<String> stopPlugin(String pluginId);

    List<String> reloadPlugin(String pluginId);
}
