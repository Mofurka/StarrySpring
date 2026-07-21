package irden.space.boot.persistence;

import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;


@Configuration
@EnableTransactionManagement(proxyTargetClass = true)
public class PluginTransactionConfiguration {
}
