package irden.space.proxy.plugin.api;

import java.util.List;
import java.util.Objects;


public final class ProxyPluginSupport {

    private ProxyPluginSupport() {
    }

    public static PluginDescriptor descriptor(ProxyPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");

        PluginDefinition definition = plugin.getClass().getAnnotation(PluginDefinition.class);
        if (definition == null) {
            throw new IllegalStateException(
                    "Plugin class " + plugin.getClass().getName()
                            + " must either override descriptor() or declare @PluginDefinition"
            );
        }

        return new PluginDescriptor(
                definition.id(),
                definition.name(),
                definition.version(),
                List.of(definition.dependsOn())
        );
    }
}
