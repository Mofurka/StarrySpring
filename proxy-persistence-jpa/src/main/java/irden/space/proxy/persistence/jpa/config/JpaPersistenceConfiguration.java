package irden.space.proxy.persistence.jpa.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = "irden.space.proxy.persistence.jpa.entity")
@EnableJpaRepositories(basePackages = "irden.space.proxy.persistence.jpa.repository")
public class JpaPersistenceConfiguration {
}