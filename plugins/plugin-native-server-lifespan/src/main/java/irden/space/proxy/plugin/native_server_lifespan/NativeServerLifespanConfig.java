package irden.space.proxy.plugin.native_server_lifespan;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.time.Duration;

@ConfigurationProperties(prefix = "native-server-lifespan")
public record NativeServerLifespanConfig(
        boolean enabled,
        Path gameDirectoryPath,
        Rcon rcon
) {

    public record Rcon(
            boolean enabled,
            String host,
            int port,
            String password,
            Duration connectTimeout,
            Duration readTimeout,
            Duration shutdownTimeout
    ) {
    }
}
