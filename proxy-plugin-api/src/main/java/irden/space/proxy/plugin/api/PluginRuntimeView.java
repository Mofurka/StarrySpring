package irden.space.proxy.plugin.api;

import java.util.Objects;

public record PluginRuntimeView(
        PluginDescriptor descriptor,
        PluginRuntimeState state
) {
    public PluginRuntimeView {
        Objects.requireNonNull(descriptor, "descriptor");
        Objects.requireNonNull(state, "state");
    }
}
