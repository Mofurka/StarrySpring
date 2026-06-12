package irden.space.proxy.plugin.runtime;

import java.util.Collection;
import java.util.Map;

public interface PluginServiceProvider {

    Map<Class<?>, Object> servicesPublishedBy(Collection<String> pluginIds);
}
