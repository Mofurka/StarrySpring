package irden.space.proxy.plugin.osb_detector;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("osb-detector")
public record OsbDetectorConfiguration(
        boolean enabled,
        Integer osbProtocolVersionThreshold,
        String osbVersion

) {
}
