package irden.space.proxy.plugin.api;

import java.util.Objects;

public record PluginRuntimeView(
        PluginDescriptor descriptor,
        PluginRuntimeState state,
        PluginFailure failure
) {
    public PluginRuntimeView {
        Objects.requireNonNull(descriptor, "descriptor");
        Objects.requireNonNull(state, "state");
    }

    public PluginRuntimeView(PluginDescriptor descriptor, PluginRuntimeState state) {
        this(descriptor, state, null);
    }
}
