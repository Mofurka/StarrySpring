package irden.space.proxy.plugin.irden;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;


@ConfigurationProperties(prefix = "irden")
public record IrdenConfig(
        WeatherProperties weather
) {

    public record WeatherProperties(
            String configLocation,
            Path statePath,
            String zone
    ) {
    }
}
