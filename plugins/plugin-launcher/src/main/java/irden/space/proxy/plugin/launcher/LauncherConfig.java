package irden.space.proxy.plugin.launcher;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "launcher")
public record LauncherConfig(
        boolean enabled,
        String bearerToken
) {
}
