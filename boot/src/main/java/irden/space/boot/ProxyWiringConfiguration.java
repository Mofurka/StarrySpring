package irden.space.boot;

import irden.space.proxy.adapters.config.ProxyAdaptersConfiguration;
import irden.space.proxy.application.port.out.SessionRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProxyWiringConfiguration {

    @Bean
    public SessionRegistry sessionRegistry() {
        return ProxyAdaptersConfiguration.sessionRegistry();
    }
}

