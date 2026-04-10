package irden.space.proxy.application;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ProxyServerProperties.class)
public class ProxyRuntimeConfiguration {
}
