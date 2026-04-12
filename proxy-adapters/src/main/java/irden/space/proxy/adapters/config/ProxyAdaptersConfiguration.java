package irden.space.proxy.adapters.config;

import irden.space.proxy.adapters.session.InMemorySessionRegistry;
import irden.space.proxy.application.port.out.SessionRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProxyAdaptersConfiguration {

    @Bean
    public SessionRegistry sessionRegistry() {
        return new InMemorySessionRegistry();
    }
}
