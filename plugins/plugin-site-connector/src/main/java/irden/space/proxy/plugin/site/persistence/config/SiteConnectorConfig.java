package irden.space.proxy.plugin.site.persistence.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;


@ConfigurationProperties("site-connector")
public record SiteConnectorConfig(
        String apiKey,
        String baseUrl,
        Duration connectTimeout,
        Duration readTimeout
) {
}
