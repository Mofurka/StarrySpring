package irden.space.proxy.plugin.runtime;

import irden.space.proxy.plugin.api.PluginDefinition;
import irden.space.proxy.plugin.api.PluginDescriptor;
import irden.space.proxy.plugin.api.ProxyPlugin;

import java.util.List;
import java.util.Objects;

public record PluginCandidate(
        Class<? extends ProxyPlugin> pluginClass,
        PluginDescriptor descriptor
) {

    public PluginCandidate {
        Objects.requireNonNull(pluginClass, "pluginClass");
        Objects.requireNonNull(descriptor, "descriptor");
    }

    public static PluginCandidate fromClass(Class<? extends ProxyPlugin> pluginClass) {
        PluginDefinition definition = pluginClass.getAnnotation(PluginDefinition.class);
        if (definition == null) {
            throw new IllegalStateException(
                    "Plugin class " + pluginClass.getName()
                            + " must declare @PluginDefinition when discovered without creating an instance"
            );
        }
        return new PluginCandidate(
                pluginClass,
                new PluginDescriptor(
                        definition.id(),
                        definition.name(),
                        definition.version(),
                        List.of(definition.dependsOn())
                )
        );
    }
}
