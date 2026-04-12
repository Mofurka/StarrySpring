package irden.space.proxy.plugin.api;


import java.util.List;

public record PluginDescriptor(
        String id,
        String name,
        String version,
        List<String> dependsOn
) {
}
