package irden.space.proxy.plugin.site.persistence.config;

import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("site-connector")
@Getter
@Setter
public class SiteConnectorConfig {
    private String apiKey = "";
    private String siteUrl = "";
}
