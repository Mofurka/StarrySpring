package irden.space.proxy.plugin.runtime;

import irden.space.proxy.plugin.api.PluginContext;

import java.util.List;

@FunctionalInterface
public interface PluginContainerFactory {

    PluginContainer create(
            PluginCandidate candidate,
            PluginContext pluginContext,
            List<PluginContainer> dependencyContainers
    );
}
