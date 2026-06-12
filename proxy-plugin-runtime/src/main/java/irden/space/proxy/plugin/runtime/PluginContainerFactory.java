package irden.space.proxy.plugin.runtime;

import irden.space.proxy.plugin.api.PluginContext;

@FunctionalInterface
public interface PluginContainerFactory {

    PluginContainer create(PluginCandidate candidate, PluginContext pluginContext);
}
