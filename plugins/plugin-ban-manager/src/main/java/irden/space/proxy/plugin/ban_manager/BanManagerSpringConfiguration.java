package irden.space.proxy.plugin.ban_manager;

import irden.space.proxy.plugin.ban_manager.persistence.BanRecordJdbcRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@Import(BanRecordJdbcRepository.class)
public class BanManagerSpringConfiguration {
}
