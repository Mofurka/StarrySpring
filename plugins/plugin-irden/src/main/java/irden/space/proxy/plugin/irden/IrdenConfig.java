package irden.space.proxy.plugin.irden;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.time.Duration;


@ConfigurationProperties(prefix = "irden")
public record IrdenConfig(
        WeatherProperties weather,
        SiteConnectorConfig siteConnector
) {

    public record WeatherProperties(
            String configLocation,
            Path statePath,
            String zone,
            Long announceDiscordChannelId
    ) {
    }

    public record SiteConnectorConfig(
            String apiKey,
            String baseUrl,
            Duration connectTimeout,
            Duration readTimeout,
            String inboundApiKey
    ) {
    }
}
