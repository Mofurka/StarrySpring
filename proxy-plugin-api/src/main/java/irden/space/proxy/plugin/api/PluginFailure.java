package irden.space.proxy.plugin.api;

import java.time.Instant;
import java.util.Objects;

public record PluginFailure(String pluginId, String phase, String message, Instant timestamp) {

    public PluginFailure {
        Objects.requireNonNull(pluginId, "pluginId");
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(timestamp, "timestamp");
    }

    public static PluginFailure of(String pluginId, String phase, Throwable error) {
        Objects.requireNonNull(error, "error");
        String message = error.getMessage() != null ? error.getMessage() : error.getClass().getName();
        return new PluginFailure(pluginId, phase, message, Instant.now());
    }
}
